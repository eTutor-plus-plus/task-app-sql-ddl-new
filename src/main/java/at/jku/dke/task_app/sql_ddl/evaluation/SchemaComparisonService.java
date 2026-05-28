package at.jku.dke.task_app.sql_ddl.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        return extractForeignKeys(expectedSchema).keySet().equals(extractForeignKeys(actualSchema).keySet());
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
        SortedMap<String, String> expected = extractForeignKeys(expectedSchema);
        SortedMap<String, String> actual = extractForeignKeys(actualSchema);
        int matches = 0;
        for (String entry : expected.keySet()) {
            if (actual.containsKey(entry)) {
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

    public List<String> matchingForeignKeyDescriptions(JsonNode expectedSchema, JsonNode actualSchema) {
        SortedMap<String, String> expected = extractForeignKeys(expectedSchema);
        SortedMap<String, String> actual = extractForeignKeys(actualSchema);
        return expected.entrySet().stream()
            .filter(entry -> actual.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();
    }

    public List<String> mismatchingForeignKeyDescriptions(JsonNode expectedSchema, JsonNode actualSchema) {
        SortedMap<String, String> expected = extractForeignKeys(expectedSchema);
        SortedMap<String, String> actual = extractForeignKeys(actualSchema);
        SortedMap<String, String> mismatches = new TreeMap<>();

        expected.forEach((key, value) -> {
            if (!actual.containsKey(key)) {
                mismatches.put(key, value);
            }
        });

        actual.forEach((key, value) -> {
            if (!expected.containsKey(key)) {
                mismatches.put(key, value);
            }
        });

        return List.copyOf(mismatches.values());
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

    private SortedMap<String, String> extractForeignKeys(JsonNode schemaNode) {
        SortedMap<String, String> result = new TreeMap<>();
        for (JsonNode table : safeArray(schemaNode.path("tables"))) {
            String tableName = table.path("name").asText("");
            result.putAll(extractForeignKeys(tableName, table.path("foreignKeys")));
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
        return node::elements;
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

    private boolean isGroupedForeignKeyNode(JsonNode foreignKey) {
        return foreignKey.path("columns").isArray() && foreignKey.path("referencedColumns").isArray();
    }

    private SortedMap<String, String> extractForeignKeys(String tableName, JsonNode foreignKeysNode) {
        SortedMap<String, String> result = new TreeMap<>();
        Map<String, List<String>> groupedColumnPairs = new LinkedHashMap<>();
        Map<String, String> groupedReferencedTables = new HashMap<>();
        Map<String, String> groupedUpdateRules = new HashMap<>();
        Map<String, String> groupedDeleteRules = new HashMap<>();
        int syntheticForeignKeyIndex = 0;

        for (JsonNode fk : safeArray(foreignKeysNode)) {
            if (isGroupedForeignKeyNode(fk)) {
                List<String> columnPairs = zipColumnPairs(fk.path("columns"), fk.path("referencedColumns"));
                String normalizedKey = buildForeignKeyKey(
                    tableName,
                    fk.path("referencedTable").asText(""),
                    fk.path("updateRule").asText(""),
                    fk.path("deleteRule").asText(""),
                    columnPairs
                );
                result.put(normalizedKey, buildForeignKeyDisplay(tableName, columnPairs));
                continue;
            }

            String foreignKeyName = fk.path("name").asText("");
            String groupKey = foreignKeyName.isBlank()
                ? "synthetic-" + syntheticForeignKeyIndex++
                : foreignKeyName;
            groupedColumnPairs.computeIfAbsent(groupKey, ignored -> new ArrayList<>())
                .add(joinColumnPair(fk.path("column").asText(""), fk.path("referencedColumn").asText("")));
            groupedReferencedTables.putIfAbsent(groupKey, fk.path("referencedTable").asText(""));
            groupedUpdateRules.putIfAbsent(groupKey, fk.path("updateRule").asText(""));
            groupedDeleteRules.putIfAbsent(groupKey, fk.path("deleteRule").asText(""));
        }

        groupedColumnPairs.forEach((groupKey, columnPairs) -> {
            String normalizedKey = buildForeignKeyKey(
                tableName,
                groupedReferencedTables.getOrDefault(groupKey, ""),
                groupedUpdateRules.getOrDefault(groupKey, ""),
                groupedDeleteRules.getOrDefault(groupKey, ""),
                columnPairs
            );
            result.put(normalizedKey, buildForeignKeyDisplay(tableName, columnPairs));
        });

        return result;
    }

    private List<String> readColumns(JsonNode columnsNode) {
        List<String> columns = new ArrayList<>();
        for (JsonNode column : safeArray(columnsNode)) {
            columns.add(column.asText(""));
        }
        return columns;
    }

    private List<String> zipColumnPairs(JsonNode columnsNode, JsonNode referencedColumnsNode) {
        List<String> columns = readColumns(columnsNode);
        List<String> referencedColumns = readColumns(referencedColumnsNode);
        List<String> columnPairs = new ArrayList<>();
        int pairCount = Math.min(columns.size(), referencedColumns.size());

        for (int i = 0; i < pairCount; i++) {
            columnPairs.add(joinColumnPair(columns.get(i), referencedColumns.get(i)));
        }

        return columnPairs;
    }

    private String buildForeignKeyKey(
        String tableName,
        String referencedTable,
        String updateRule,
        String deleteRule,
        List<String> columnPairs
    ) {
        List<String> sortedPairs = columnPairs.stream()
            .sorted()
            .toList();
        return String.join("|",
            tableName,
            String.join(",", sortedPairs),
            referencedTable,
            updateRule,
            deleteRule
        );
    }

    private String buildForeignKeyDisplay(String tableName, List<String> columnPairs) {
        List<String> sortedColumns = columnPairs.stream()
            .sorted()
            .map(this::extractForeignKeyColumn)
            .toList();
        return tableName + "(" + String.join(", ", sortedColumns) + ")";
    }

    private String joinColumnPair(String column, String referencedColumn) {
        return column + "|" + referencedColumn;
    }

    private String extractForeignKeyColumn(String columnPair) {
        int separatorIndex = columnPair.indexOf('|');
        if (separatorIndex < 0) {
            return columnPair;
        }
        return columnPair.substring(0, separatorIndex);
    }
}
