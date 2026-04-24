package at.jku.dke.task_app.sql_ddl.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Set;
import java.util.TreeMap;
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

    public int countExpectedTables(JsonNode expectedSchema) {
        return extractTablesWithColumns(expectedSchema).size();
    }

    public int countMatchingTables(JsonNode expectedSchema, JsonNode actualSchema) {
        Map<String, Set<String>> expected = extractTablesWithColumns(expectedSchema);
        Map<String, Set<String>> actual = extractTablesWithColumns(actualSchema);
        int matches = 0;
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            if (entry.getValue().equals(actual.get(entry.getKey()))) {
                matches++;
            }
        }
        return matches;
    }

    public int countExpectedPrimaryKeys(JsonNode expectedSchema) {
        return extractPrimaryKeys(expectedSchema).size();
    }

    public int countMatchingPrimaryKeys(JsonNode expectedSchema, JsonNode actualSchema) {
        Map<String, Set<String>> expected = extractPrimaryKeys(expectedSchema);
        Map<String, Set<String>> actual = extractPrimaryKeys(actualSchema);
        int matches = 0;
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            if (entry.getValue().equals(actual.get(entry.getKey()))) {
                matches++;
            }
        }
        return matches;
    }

    public int countExpectedForeignKeys(JsonNode expectedSchema) {
        return extractForeignKeys(expectedSchema).size();
    }

    public int countMatchingForeignKeys(JsonNode expectedSchema, JsonNode actualSchema) {
        Set<String> expected = extractForeignKeys(expectedSchema);
        Set<String> actual = extractForeignKeys(actualSchema);
        int matches = 0;
        for (String entry : expected) {
            if (actual.contains(entry)) {
                matches++;
            }
        }
        return matches;
    }

    public int countExpectedUniqueConstraints(JsonNode expectedSchema) {
        return extractUniqueConstraints(expectedSchema).values().stream().mapToInt(Set::size).sum();
    }

    public int countMatchingUniqueConstraints(JsonNode expectedSchema, JsonNode actualSchema) {
        Map<String, Set<String>> expected = extractUniqueConstraints(expectedSchema);
        Map<String, Set<String>> actual = extractUniqueConstraints(actualSchema);

        int matches = 0;
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            Set<String> actualValues = actual.getOrDefault(entry.getKey(), Set.of());
            for (String value : entry.getValue()) {
                if (actualValues.contains(value)) {
                    matches++;
                }
            }
        }
        return matches;
    }

    public List<String> matchingTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return matchingKeys(extractTablesWithColumns(expectedSchema), extractTablesWithColumns(actualSchema));
    }

    public List<String> mismatchingTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return mismatchingKeys(extractTablesWithColumns(expectedSchema), extractTablesWithColumns(actualSchema));
    }

    public List<String> matchingPrimaryKeyTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return matchingKeys(extractPrimaryKeys(expectedSchema), extractPrimaryKeys(actualSchema));
    }

    public List<String> mismatchingPrimaryKeyTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return mismatchingKeys(extractPrimaryKeys(expectedSchema), extractPrimaryKeys(actualSchema));
    }

    public List<String> matchingForeignKeyTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return matchingKeys(extractForeignKeysByTable(expectedSchema), extractForeignKeysByTable(actualSchema));
    }

    public List<String> mismatchingForeignKeyTableNames(JsonNode expectedSchema, JsonNode actualSchema) {
        return mismatchingKeys(extractForeignKeysByTable(expectedSchema), extractForeignKeysByTable(actualSchema));
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

    private Map<String, Set<String>> extractForeignKeysByTable(JsonNode schemaNode) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            Set<String> descriptors = new TreeSet<>();
            for (JsonNode fk : safeArray(table.path("foreignKeys"))) {
                String descriptor = String.join("|",
                    fk.path("column").asText(""),
                    fk.path("referencedTable").asText(""),
                    fk.path("referencedColumn").asText(""),
                    fk.path("updateRule").asText(""),
                    fk.path("deleteRule").asText(""));
                descriptors.add(descriptor);
            }
            result.put(tableName, descriptors);
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

    private List<String> matchingKeys(Map<String, Set<String>> expected, Map<String, Set<String>> actual) {
        SortedMap<String, Set<String>> keys = collectKeys(expected, actual);
        return keys.keySet().stream()
            .filter(key -> expected.getOrDefault(key, Set.of()).equals(actual.getOrDefault(key, Set.of())))
            .toList();
    }

    private List<String> mismatchingKeys(Map<String, Set<String>> expected, Map<String, Set<String>> actual) {
        SortedMap<String, Set<String>> keys = collectKeys(expected, actual);
        return keys.keySet().stream()
            .filter(key -> !expected.getOrDefault(key, Set.of()).equals(actual.getOrDefault(key, Set.of())))
            .toList();
    }

    private SortedMap<String, Set<String>> collectKeys(Map<String, Set<String>> expected, Map<String, Set<String>> actual) {
        SortedMap<String, Set<String>> keys = new TreeMap<>();
        expected.keySet().forEach(key -> keys.put(key, Set.of()));
        actual.keySet().forEach(key -> keys.put(key, Set.of()));
        return keys;
    }
}
