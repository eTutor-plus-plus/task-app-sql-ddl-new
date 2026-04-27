package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLCheckConstraintDto;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import at.jku.dke.task_app.sql_ddl.evaluation.SchemaMetadataExtractor;
import at.jku.dke.task_app.sql_ddl.evaluation.feedback.WhitelistWordService;
import com.fasterxml.jackson.databind.JsonNode;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SQLDDLTaskService extends BaseTaskService<SQLDDLTask, ModifySQLDDLTaskDto> {
    private static final Logger LOG = LoggerFactory.getLogger(SQLDDLTaskService.class);

    private final MessageSource messageSource;
    private final SchemaMetadataExtractor schemaMetadataExtractor;
    private final WhitelistWordService whitelistWordService;

    public SQLDDLTaskService(
        SQLDDLTaskRepository repository,
        MessageSource messageSource,
        SchemaMetadataExtractor schemaMetadataExtractor,
        WhitelistWordService whitelistWordService
    ) {
        super(repository);
        this.messageSource = messageSource;
        this.schemaMetadataExtractor = schemaMetadataExtractor;
        this.whitelistWordService = whitelistWordService;
    }

    @Override
    protected SQLDDLTask createTask(long id, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {
        if (!dto.taskType().equals("sql-ddl")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        }

        String solution = dto.additionalData().solution();
        String whitelist = dto.additionalData().whitelist() == null || dto.additionalData().whitelist().isBlank()
            ? whitelistWordService.generateWhitelist(solution)
            : dto.additionalData().whitelist();

        ExecutedTaskArtifacts artifacts = prepareTaskArtifacts(solution, dto.additionalData().insertStatements());

        SQLDDLTask task = new SQLDDLTask(
            id,
            dto.maxPoints(),
            dto.status(),
            solution,
            artifacts.executedSolution(),
            dto.additionalData().tablePoints(),
            dto.additionalData().primaryKeyPoints(),
            dto.additionalData().foreignKeyPoints(),
            dto.additionalData().constraintPoints(),
            whitelist,
            artifacts.checkConstraints()
        );

        artifacts.checkConstraints().forEach(constraint -> constraint.setTask(task));
        return task;
    }

    @Override
    protected void afterCreate(SQLDDLTask task, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {

    }

    @Override
    protected void updateTask(SQLDDLTask task, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {
        if (!dto.taskType().equals("sql-ddl")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        }

        String solution = dto.additionalData().solution();
        String whitelist = dto.additionalData().whitelist() == null || dto.additionalData().whitelist().isBlank()
            ? whitelistWordService.generateWhitelist(solution)
            : dto.additionalData().whitelist();
        ExecutedTaskArtifacts artifacts = prepareTaskArtifacts(solution, dto.additionalData().insertStatements());

        task.setMaxPoints(dto.maxPoints());
        task.setStatus(dto.status());
        task.setSolution(solution);
        task.setExecutedSolution(artifacts.executedSolution());
        task.setTablePoints(dto.additionalData().tablePoints());
        task.setPrimaryKeyPoints(dto.additionalData().primaryKeyPoints());
        task.setForeignKeyPoints(dto.additionalData().foreignKeyPoints());
        task.setConstraintPoints(dto.additionalData().constraintPoints());
        task.setWhitelist(whitelist);
        task.getCheckConstraints().clear();
        artifacts.checkConstraints().forEach(constraint -> {
            constraint.setTask(task);
            task.getCheckConstraints().add(constraint);
        });
    }

    @Override
    protected void afterUpdate(SQLDDLTask task, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {

    }

    @Override
    public void beforeDelete(long id) {

    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(SQLDDLTask task, boolean create) {
        return new TaskModificationResponseDto(
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.ENGLISH)
        );
    }

    private ExecutedTaskArtifacts prepareTaskArtifacts(String solution, List<SQLDDLCheckConstraintDto> checkConstraintDtos) {
        if (solution == null || solution.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solution must not be blank.");
        }

        String dbName = "sql_ddl_" + UUID.randomUUID();
        // initializes the H2 in-memory database in Oracle compatibility mode
        // database is automatically deleted when the connection is closed
        String h2Url = "jdbc:h2:mem:" + dbName + ";MODE=Oracle";

        // when exiting the try block the H2 database connection is closed automatically
        try (Connection connection = DriverManager.getConnection(h2Url, "sa", "")) {
            connection.setAutoCommit(false);
            RunScript.execute(connection, new StringReader(solution));
            var executedSolution = schemaMetadataExtractor.extract(connection, "PUBLIC");
            List<SQLDDLCheckConstraint> checkConstraints = buildCheckConstraints(connection, checkConstraintDtos);
            return new ExecutedTaskArtifacts(executedSolution, checkConstraints);
        } catch (SQLException ex) {
            LOG.warn("Schema setup failed for schema PUBLIC: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided schema is not executable: " + ex.getMessage(), ex);
        }
    }

    private List<SQLDDLCheckConstraint> buildCheckConstraints(Connection connection, List<SQLDDLCheckConstraintDto> checkConstraintDtos) throws SQLException {
        List<SQLDDLCheckConstraint> checkConstraints = new ArrayList<>();
        if (checkConstraintDtos == null) {
            return checkConstraints;
        }

        for (SQLDDLCheckConstraintDto constraintDTO : checkConstraintDtos) {
            checkConstraints.add(createCheckConstraint(constraintDTO, connection));
        }
        return checkConstraints;
    }

    private SQLDDLCheckConstraint createCheckConstraint(SQLDDLCheckConstraintDto dto, Connection connection) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        executeSuccessfulStatements(dto, connection);
        executeUnsuccessfulStatements(dto, connection);
        connection.rollback(savepoint);

        return new SQLDDLCheckConstraint(
            dto.definition(),
            dto.successfulStatements(),
            dto.unsuccessfulStatements()
        );
    }

    private void executeSuccessfulStatements(SQLDDLCheckConstraintDto dto, Connection connection) throws SQLException {
        if (dto.successfulStatements() == null || dto.successfulStatements().isBlank()) {
            return;
        }

        try {
            RunScript.execute(connection, new StringReader(dto.successfulStatements()));
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Successful insert statements failed for check constraint '" + dto.definition() + "': "
                    + ex.getMessage(),
                ex
            );
        }
    }

    private void executeUnsuccessfulStatements(SQLDDLCheckConstraintDto dto, Connection connection) throws SQLException {
        if (dto.unsuccessfulStatements() == null || dto.unsuccessfulStatements().isBlank()) {
            return;
        }

        try {
            RunScript.execute(connection, new StringReader(dto.unsuccessfulStatements()));
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsuccessful insert statements unexpectedly succeeded for check constraint '" + dto.definition() + "'"
            );
        } catch (SQLException ex) {
            if (!"23513".equals(ex.getSQLState())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsuccessful insert statements failed for a reason other than a check-constraint violation for '" + dto.definition() + "': "
                        + ex.getMessage(),
                    ex
                );
            }
        }
    }

    private record ExecutedTaskArtifacts(JsonNode executedSolution, List<SQLDDLCheckConstraint> checkConstraints) {
    }
}
