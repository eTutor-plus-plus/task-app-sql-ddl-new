package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLCheckConstraintDto;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class provides methods for managing {@link SQLDDLTask}s.
 */
@Service
public class SQLDDLTaskService extends BaseTaskService<SQLDDLTask, ModifySQLDDLTaskDto> {
    private static final Logger LOG = LoggerFactory.getLogger(SQLDDLTaskService.class);

    private final MessageSource messageSource;
    private final String username;
    private final String password;
    private final String url;
    private final SQLDDLTaskRepository sqlDdlTaskRepository;

    /**
     * Creates a new instance of class {@link SQLDDLTaskService}.
     *
     * @param username      The database username.
     * @param password      The database password.
     * @param url           The database url.
     * @param repository    The task repository.
     * @param messageSource The message source.
     */
    public SQLDDLTaskService(
        @Value("${spring.datasource.username}") String username,
        @Value("${spring.datasource.password}") String password,
        @Value("${spring.datasource.url}") String url,
        SQLDDLTaskRepository repository,
        MessageSource messageSource
    ) {
        super(repository);
        this.messageSource = messageSource;
        this.username = username;
        this.password = password;
        this.url = url;
        this.sqlDdlTaskRepository = repository;
    }

    @Override
    protected SQLDDLTask createTask(long id, ModifyTaskDto<ModifySQLDDLTaskDto> dto) {
        if (!dto.taskType().equals("sql-ddl")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        }

        String solution = dto.additionalData().solution();
        String whitelist = dto.additionalData().whitelist() == null || dto.additionalData().whitelist().isBlank()
            ? generateWordlist(solution)
            : dto.additionalData().whitelist();

        SQLDDLTask task = new SQLDDLTask(
            id,
            dto.maxPoints(),
            dto.status(),
            solution,
            JsonNodeFactory.instance.objectNode(),
            dto.additionalData().tablePoints(),
            dto.additionalData().primaryKeyPoints(),
            dto.additionalData().foreignKeyPoints(),
            dto.additionalData().constraintPoints(),
            whitelist,
            new ArrayList<>()
        );

        this.replaceCheckConstraints(task, dto.additionalData().insertStatements());
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
            ? generateWordlist(solution)
            : dto.additionalData().whitelist();

        task.setMaxPoints(dto.maxPoints());
        task.setStatus(dto.status());
        task.setSolution(solution);
        task.setExecutedSolution(JsonNodeFactory.instance.objectNode());
        task.setTablePoints(dto.additionalData().tablePoints());
        task.setPrimaryKeyPoints(dto.additionalData().primaryKeyPoints());
        task.setForeignKeyPoints(dto.additionalData().foreignKeyPoints());
        task.setConstraintPoints(dto.additionalData().constraintPoints());
        task.setWhitelist(whitelist);
        this.replaceCheckConstraints(task, dto.additionalData().insertStatements());
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

    private void executeSchemaSetup(String schemaName, String solution) {

    }

    private String generateWordlist(String solution) {
        return Arrays.stream(solution.split("[^a-zA-Z0-9_]"))
            .filter(word -> !word.isBlank())
            .filter(word -> !word.matches("[0-9]+"))
            .map(String::toLowerCase)
            .distinct()
            .collect(Collectors.joining(";"));
    }

    private void replaceCheckConstraints(SQLDDLTask task, List<SQLDDLCheckConstraintDto> checkConstraintDtos) {
        if (task.getCheckConstraints() == null) {
            task.setCheckConstraints(new ArrayList<>());
        } else {
            task.getCheckConstraints().clear();
        }

        if (checkConstraintDtos == null) {
            return;
        }

        checkConstraintDtos
            .forEach(constraintDTO -> {
                SQLDDLCheckConstraint constraint = createCheckConstraint(constraintDTO);
                constraint.setTask(task);
                task.getCheckConstraints().add(constraint);
            });
    }

    private SQLDDLCheckConstraint createCheckConstraint(SQLDDLCheckConstraintDto dto) {
        return new SQLDDLCheckConstraint(dto.name(), null, null, dto.insertStatements());
    }
}
