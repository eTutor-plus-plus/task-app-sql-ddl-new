package at.jku.dke.task_app.sql_ddl.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Uses maps and sets to normalize schema metadata before comparing expected and actual schemas.
 * Map: groups extracted values by table name (e.g., table -> columns, table -> primary keys).
 * HashMap: efficient key-based access while building those per-table structures.
 * Set: enables order-independent equality checks and removes duplicates in descriptors.
 * TreeSet: keeps deterministic ordering for descriptors and column lists during normalization.
 * HashSet: stores normalized unique constraints where ordering is irrelevant after normalization.
 */
@Service
public class SchemaComparisonService {


    public boolean tablesMatch(JsonNode expectedSchema, JsonNode actualSchema) {
        return extractTablesWithColumns(expectedSchema).equals(extractTablesWithColumns(actualSchema));
    }

    public boolean primaryKeysMatch(JsonNode expectedSchema, JsonNode actualSchema) {
        return extractPrimaryKeys(expectedSchema).equals(extractPrimaryKeys(actualSchema));
    }

    public boolean foreignKeysMatch(JsonNode expectedSchema, JsonNode actualSchema) {
        return extractForeignKeys(expectedSchema).equals(extractForeignKeys(actualSchema));
    }

    public boolean uniqueConstraintsMatch(JsonNode expectedSchema, JsonNode actualSchema) {
        return extractUniqueConstraints(expectedSchema).equals(extractUniqueConstraints(actualSchema));
    }

    private Map<String, Set<String>> extractTablesWithColumns(JsonNode schemaNode) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            Set<String> columns = new TreeSet<>();
            for (JsonNode column : safeArray(table.path("columns"))) {
                String descriptor = String.join("|",
                    column.path("name").asText(""),
                    column.path("typeName").asText(""),
                    String.valueOf(column.path("size").asInt()),
                    String.valueOf(column.path("decimalDigits").asInt()),
                    String.valueOf(column.path("nullable").asBoolean()));
                columns.add(descriptor);
            }
            result.put(tableName, columns);
        }
        return result;
    }

    private Map<String, Set<String>> extractPrimaryKeys(JsonNode schemaNode) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            Set<String> pkColumns = new TreeSet<>();
            for (JsonNode column : safeArray(table.path("primaryKey").path("columns"))) {
                pkColumns.add(column.asText(""));
            }
            result.put(tableName, pkColumns);
        }
        return result;
    }

    private Set<String> extractForeignKeys(JsonNode schemaNode) {
        Set<String> result = new TreeSet<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            for (JsonNode fk : safeArray(table.path("foreignKeys"))) {
                String descriptor = String.join("|",
                    tableName,
                    fk.path("column").asText(""),
                    fk.path("referencedTable").asText(""),
                    fk.path("referencedColumn").asText(""),
                    fk.path("updateRule").asText(""),
                    fk.path("deleteRule").asText(""));
                result.add(descriptor);
            }
        }
        return result;
    }

    private Map<String, Set<String>> extractUniqueConstraints(JsonNode schemaNode) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            Set<String> normalizedConstraints = new HashSet<>();
            for (JsonNode unique : safeArray(table.path("uniqueConstraints"))) {
                Set<String> columns = new TreeSet<>();
                for (JsonNode column : safeArray(unique.path("columns"))) {
                    columns.add(column.asText(""));
                }
                normalizedConstraints.add(String.join(",", columns));
            }
            result.put(tableName, normalizedConstraints);
        }
        return result;
    }

    private Iterable<JsonNode> safeArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        return () -> node.elements();
    }
}
