package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLCheckConstraintDto;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides methods for managing {@link SQLDDLTask}s.
 */
@Service
public class SQLDDLTaskService extends BaseTaskService<SQLDDLTask, ModifySQLDDLTaskDto> {
    private static final Logger LOG = LoggerFactory.getLogger(SQLDDLTaskService.class);

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link SQLDDLTaskService}.
     *
     * @param repository    The task repository.
     * @param messageSource The message source.
     */
    public SQLDDLTaskService(
        SQLDDLTaskRepository repository,
        MessageSource messageSource
    ) {
        super(repository);
        this.messageSource = messageSource;
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
            ? generateWordlist(solution)
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
            ObjectNode executedSolution = extractSchemaMetadata(connection, "PUBLIC");
            List<SQLDDLCheckConstraint> checkConstraints = buildCheckConstraints(connection, checkConstraintDtos);
            return new ExecutedTaskArtifacts(executedSolution, checkConstraints);
        } catch (SQLException ex) {
            LOG.warn("Schema setup failed for schema PUBLIC: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided schema is not executable: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode extractSchemaMetadata(Connection connection, String schemaName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        ObjectNode schemaMetadata = JsonNodeFactory.instance.objectNode();
        schemaMetadata.put("schema", schemaName);
        ArrayNode tablesNode = schemaMetadata.putArray("tables");

        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, schemaName, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        for (String tableName : tables) {
            ObjectNode tableNode = tablesNode.addObject();
            tableNode.put("name", tableName);
            tableNode.set("columns", extractColumns(metaData, schemaName, tableName));
            tableNode.set("primaryKey", extractPrimaryKey(metaData, schemaName, tableName));
            tableNode.set("foreignKeys", extractForeignKeys(metaData, schemaName, tableName));
            tableNode.set("uniqueConstraints", extractUniqueConstraints(metaData, schemaName, tableName));
        }

        return schemaMetadata;
    }

    private ArrayNode extractColumns(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        ArrayNode columnsNode = JsonNodeFactory.instance.arrayNode();
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
                ObjectNode column = columnsNode.addObject();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("typeName", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("decimalDigits", rs.getInt("DECIMAL_DIGITS"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("ordinalPosition", rs.getInt("ORDINAL_POSITION"));
            }
        }
        return columnsNode;
    }

    private ObjectNode extractPrimaryKey(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        String primaryKeyName = null;
        List<String> columns = new ArrayList<>();

        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                primaryKeyName = rs.getString("PK_NAME");
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }

        ObjectNode pkNode = JsonNodeFactory.instance.objectNode();

        pkNode.put("name", primaryKeyName);
        ArrayNode columnNames = pkNode.putArray("columns");
        columns.forEach(columnNames::add);
        return pkNode;
    }

    private ArrayNode extractForeignKeys(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        ArrayNode foreignKeysNode = JsonNodeFactory.instance.arrayNode();
        try (ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                ObjectNode foreignKey = foreignKeysNode.addObject();
                foreignKey.put("name", rs.getString("FK_NAME"));
                foreignKey.put("column", rs.getString("FKCOLUMN_NAME"));
                foreignKey.put("referencedTable", rs.getString("PKTABLE_NAME"));
                foreignKey.put("referencedColumn", rs.getString("PKCOLUMN_NAME"));
                foreignKey.put("updateRule", fkRuleToText(rs.getShort("UPDATE_RULE")));
                foreignKey.put("deleteRule", fkRuleToText(rs.getShort("DELETE_RULE")));
            }
        }
        return foreignKeysNode;
    }

    private ArrayNode extractUniqueConstraints(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        Map<String, List<String>> uniqueConstraints = new HashMap<>();
        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, tableName, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                int indexType = rs.getShort("TYPE");

                if (indexName == null || columnName == null || indexType == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }

                uniqueConstraints
                    .computeIfAbsent(indexName, key -> new ArrayList<>())
                    .add(columnName);
            }
        }

        ArrayNode uniqueConstraintsNode = JsonNodeFactory.instance.arrayNode();
        uniqueConstraints.forEach((key, value) -> {
            ObjectNode uniqueConstraint = uniqueConstraintsNode.addObject();
            uniqueConstraint.put("name", key);
            ArrayNode columns = uniqueConstraint.putArray("columns");
            value.forEach(columns::add);
        });
        return uniqueConstraintsNode;
    }

    private String fkRuleToText(short rule) {
        return switch (rule) {
            case DatabaseMetaData.importedKeyNoAction -> "NO_ACTION";
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeySetNull -> "SET_NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET_DEFAULT";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            default -> "UNKNOWN";
        };
    }

    private String generateWordlist(String solution) {
        return Arrays.stream(solution.split("[^a-zA-Z0-9_]"))
            .filter(word -> !word.isBlank())
            .filter(word -> !word.matches("[0-9]+"))
            .map(String::toLowerCase)
            .distinct()
            .collect(Collectors.joining(";"));
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
            dto.checkDefinition(),
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
                "Successful insert statements failed for check constraint '" + dto.checkDefinition() + "': "
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
                "Unsuccessful insert statements unexpectedly succeeded for check constraint '" + dto.checkDefinition() + "'"
            );
        } catch (SQLException ex) {
            if (!"23513".equals(ex.getSQLState())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsuccessful insert statements failed for a reason other than a check-constraint violation for '" + dto.checkDefinition() + "': "
                        + ex.getMessage(),
                    ex
                );
            }
        }
    }

    private record ExecutedTaskArtifacts(ObjectNode executedSolution, List<SQLDDLCheckConstraint> checkConstraints) {
    }
}
