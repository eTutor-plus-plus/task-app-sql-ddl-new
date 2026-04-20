package at.jku.dke.task_app.sql_ddl.evaluation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SchemaMetadataExtractor {

    public ObjectNode extract(Connection connection, String schemaName) throws SQLException {
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
            Set<String> foreignKeyColumns = extractForeignKeyColumns(metaData, schemaName, tableName);
            ObjectNode tableNode = tablesNode.addObject();
            tableNode.put("name", tableName);
            tableNode.set("columns", extractColumns(metaData, schemaName, tableName, foreignKeyColumns));
            tableNode.set("primaryKey", extractPrimaryKey(metaData, schemaName, tableName));
            tableNode.set("foreignKeys", extractForeignKeys(metaData, schemaName, tableName));
            tableNode.set("uniqueConstraints", extractUniqueConstraints(metaData, schemaName, tableName));
        }

        return schemaMetadata;
    }

    private ArrayNode extractColumns(
        DatabaseMetaData metaData,
        String schemaName,
        String tableName,
        Set<String> foreignKeyColumns
    ) throws SQLException {
        ArrayNode columnsNode = JsonNodeFactory.instance.arrayNode();
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName != null && foreignKeyColumns.contains(columnName)) {
                    continue;
                }

                ObjectNode column = columnsNode.addObject();
                column.put("name", columnName);
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

    private Set<String> extractForeignKeyColumns(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        Set<String> foreignKeyColumns = new HashSet<>();
        try (ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                if (columnName != null) {
                    foreignKeyColumns.add(columnName);
                }
            }
        }
        return foreignKeyColumns;
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

                uniqueConstraints.computeIfAbsent(indexName, key -> new ArrayList<>()).add(columnName);
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
}
