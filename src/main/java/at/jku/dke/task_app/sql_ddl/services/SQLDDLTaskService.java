package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLAssertion;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLAssertionDto;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLCheckConstraintDto;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import at.jku.dke.task_app.sql_ddl.evaluation.SchemaMetadataExtractor;
import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.ExtractedAssertion;
import at.jku.dke.task_app.sql_ddl.evaluation.model.evaluation.PreprocessingResult;
import at.jku.dke.task_app.sql_ddl.services.feedback.WhitelistWordService;
import at.jku.dke.task_app.sql_ddl.services.assertion.AssertionConditionEvaluator;
import at.jku.dke.task_app.sql_ddl.services.assertion.AssertionScriptPreprocessor;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SQLDDLTaskService extends BaseTaskService<SQLDDLTask, ModifySQLDDLTaskDto> {
    private static final Logger LOG = LoggerFactory.getLogger(SQLDDLTaskService.class);

    private final MessageSource messageSource;
    private final SchemaMetadataExtractor schemaMetadataExtractor;
    private final WhitelistWordService whitelistWordService;
    private final AssertionScriptPreprocessor assertionScriptPreprocessor;
    private final AssertionConditionEvaluator assertionConditionEvaluator;

    public SQLDDLTaskService(
        SQLDDLTaskRepository repository,
        MessageSource messageSource,
        SchemaMetadataExtractor schemaMetadataExtractor,
        WhitelistWordService whitelistWordService,
        AssertionScriptPreprocessor assertionScriptPreprocessor,
        AssertionConditionEvaluator assertionConditionEvaluator
    ) {
        super(repository);
        this.messageSource = messageSource;
        this.schemaMetadataExtractor = schemaMetadataExtractor;
        this.whitelistWordService = whitelistWordService;
        this.assertionScriptPreprocessor = assertionScriptPreprocessor;
        this.assertionConditionEvaluator = assertionConditionEvaluator;
    }

    @Override
    protected SQLDDLTask createTask(long id, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {
        if (!dto.taskType().equals("sql-ddl")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        }

        String solution = dto.additionalData().solution();
        String whitelist = whitelistWordService.generateWhitelist(solution, dto.additionalData().whitelist());

        ExecutedTaskArtifacts artifacts = prepareTaskArtifacts(
            solution,
            dto.additionalData().insertStatements(),
            dto.additionalData().assertionStatements()
        );

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
            dto.additionalData().assertionPoints(),
            whitelist,
            artifacts.checkConstraints(),
            artifacts.assertions()
        );

        artifacts.checkConstraints().forEach(constraint -> constraint.setTask(task));
        artifacts.assertions().forEach(assertion -> assertion.setTask(task));
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
        String whitelist = whitelistWordService.generateWhitelist(solution, dto.additionalData().whitelist());
        ExecutedTaskArtifacts artifacts = prepareTaskArtifacts(
            solution,
            dto.additionalData().insertStatements(),
            dto.additionalData().assertionStatements()
        );

        task.setMaxPoints(dto.maxPoints());
        task.setStatus(dto.status());
        task.setSolution(solution);
        task.setExecutedSolution(artifacts.executedSolution());
        task.setTablePoints(dto.additionalData().tablePoints());
        task.setPrimaryKeyPoints(dto.additionalData().primaryKeyPoints());
        task.setForeignKeyPoints(dto.additionalData().foreignKeyPoints());
        task.setConstraintPoints(dto.additionalData().constraintPoints());
        task.setAssertionPoints(dto.additionalData().assertionPoints());
        task.setWhitelist(whitelist);
        task.getCheckConstraints().clear();
        artifacts.checkConstraints().forEach(constraint -> {
            constraint.setTask(task);
            task.getCheckConstraints().add(constraint);
        });
        task.getAssertions().clear();
        artifacts.assertions().forEach(assertion -> {
            assertion.setTask(task);
            task.getAssertions().add(assertion);
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

    private ExecutedTaskArtifacts prepareTaskArtifacts(
        String solution,
        List<SQLDDLCheckConstraintDto> checkConstraintDtos,
        List<SQLDDLAssertionDto> assertionDtos
    ) {
        if (solution == null || solution.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solution must not be blank.");
        }

        PreprocessingResult preprocessingResult = assertionScriptPreprocessor.preprocess(solution);
        if (!preprocessingResult.errors().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provided schema contains unsupported assertions: " + String.join(" ", preprocessingResult.errors())
            );
        }

        String dbName = "sql_ddl_" + UUID.randomUUID();
        // initializes the H2 in-memory database in Oracle compatibility mode
        // database is automatically deleted when the connection is closed
        String h2Url = "jdbc:h2:mem:" + dbName + ";MODE=Oracle";

        // when exiting the try block the H2 database connection is closed automatically
        try (Connection connection = DriverManager.getConnection(h2Url, "sa", "")) {
            connection.setAutoCommit(false);
            RunScript.execute(connection, new StringReader(preprocessingResult.sanitizedDdl()));
            var executedSolution = schemaMetadataExtractor.extract(connection, "PUBLIC");
            List<SQLDDLCheckConstraint> checkConstraints = buildCheckConstraints(connection, checkConstraintDtos);
            List<SQLDDLAssertion> assertions = buildAssertions(connection, preprocessingResult.assertions(), assertionDtos);
            return new ExecutedTaskArtifacts(executedSolution, checkConstraints, assertions);
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

    private List<SQLDDLAssertion> buildAssertions(
        Connection connection,
        List<ExtractedAssertion> extractedAssertions,
        List<SQLDDLAssertionDto> assertionDtos
    ) throws SQLException {
        List<SQLDDLAssertion> assertions = new ArrayList<>();
        if ((extractedAssertions == null || extractedAssertions.isEmpty())
            && (assertionDtos == null || assertionDtos.isEmpty())) {
            return assertions;
        }

        Map<String, SQLDDLAssertionDto> assertionNameMap = new LinkedHashMap<>();
        if (assertionDtos != null) {
            for (SQLDDLAssertionDto assertionDto : assertionDtos) {
                String normalizedName = AssertionScriptPreprocessor.normalizeName(assertionDto.definition());
                if (assertionNameMap.putIfAbsent(normalizedName, assertionDto) != null) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate assertion fixture definition '" + assertionDto.definition() + "'."
                    );
                }
            }
        }

        for (ExtractedAssertion extractedAssertion : extractedAssertions) {
            String normalizedName = AssertionScriptPreprocessor.normalizeName(extractedAssertion.name());
            SQLDDLAssertionDto assertionDto = assertionNameMap.remove(normalizedName);
            if (assertionDto == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing assertion fixture for assertion '" + extractedAssertion.name() + "'."
                );
            }

            assertions.add(createAssertion(extractedAssertion, assertionDto, connection));
        }

        if (!assertionNameMap.isEmpty()) {
            SQLDDLAssertionDto remainingFixture = assertionNameMap.values().iterator().next();
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Assertion fixture '" + remainingFixture.definition() + "' has no matching assertion in the solution."
            );
        }

        return assertions;
    }

    private SQLDDLCheckConstraint createCheckConstraint(SQLDDLCheckConstraintDto dto, Connection connection) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        executeSuccessfulStatements(dto, connection, "check constraint");
        executeUnsuccessfulStatements(dto, connection, "check constraint");
        connection.rollback(savepoint);

        return new SQLDDLCheckConstraint(
            dto.definition(),
            dto.successfulStatements(),
            dto.unsuccessfulStatements()
        );
    }

    private SQLDDLAssertion createAssertion(
        ExtractedAssertion extractedAssertion,
        SQLDDLAssertionDto dto,
        Connection connection
    ) {
        validateAssertionSuccessfulStatements(dto, extractedAssertion.definitionSql(), connection);
        validateAssertionUnsuccessfulStatements(dto, extractedAssertion.definitionSql(), connection);

        return new SQLDDLAssertion(
            extractedAssertion.name(),
            extractedAssertion.definitionSql(),
            dto.successfulStatements(),
            dto.unsuccessfulStatements()
        );
    }

    private void executeSuccessfulStatements(SQLDDLCheckConstraintDto dto, Connection connection, String statementType) throws SQLException {
        if (dto.successfulStatements() == null || dto.successfulStatements().isBlank()) {
            return;
        }

        try {
            RunScript.execute(connection, new StringReader(dto.successfulStatements()));
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Successful insert statements failed for " + statementType + " '" + dto.definition() + "': "
                    + ex.getMessage(),
                ex
            );
        }
    }

    private void executeUnsuccessfulStatements(SQLDDLCheckConstraintDto dto, Connection connection, String statementType) throws SQLException {
        if (dto.unsuccessfulStatements() == null || dto.unsuccessfulStatements().isBlank()) {
            return;
        }

        try {
            RunScript.execute(connection, new StringReader(dto.unsuccessfulStatements()));
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsuccessful insert statements unexpectedly succeeded for " + statementType + " '" + dto.definition() + "'"
            );
        } catch (SQLException ex) {
            if (!"23513".equals(ex.getSQLState())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsuccessful insert statements failed for a reason other than a check-constraint violation for " + statementType + " '" + dto.definition() + "': "
                        + ex.getMessage(),
                    ex
                );
            }
        }
    }

    private void validateAssertionSuccessfulStatements(
        SQLDDLAssertionDto dto,
        String definitionSql,
        Connection connection
    ) {
        try {
            if (!assertionConditionEvaluator.matchesExpectedOutcomeForAllStatement(
                connection,
                dto.successfulStatements(),
                definitionSql,
                true
            )) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Successful statements did not satisfy assertion '" + dto.definition() + "'."
                );
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Successful statements failed for assertion '" + dto.definition() + "': " + ex.getMessage(),
                ex
            );
        }
    }

    private void validateAssertionUnsuccessfulStatements(
        SQLDDLAssertionDto dto,
        String definitionSql,
        Connection connection
    ) {
        try {
            if (!assertionConditionEvaluator.matchesExpectedOutcomeForEachStatement(
                connection,
                dto.unsuccessfulStatements(),
                definitionSql,
                false
            )) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsuccessful statements did not violate assertion '" + dto.definition() + "'."
                );
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsuccessful statements failed for assertion '" + dto.definition() + "': " + ex.getMessage(),
                ex
            );
        }
    }

    private record ExecutedTaskArtifacts(
        JsonNode executedSolution,
        List<SQLDDLCheckConstraint> checkConstraints,
        List<SQLDDLAssertion> assertions
    ) {
    }
}
