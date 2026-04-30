package at.jku.dke.task_app.sql_ddl.evaluation.model.assertion;

/**
 * Represents the assertion extracted from a provided ddl script.
 *
 * @param name   The name of the assertion.
 * @param definitionSql The extracted CHECK definition.
 */
public record ExtractedAssertion(
    String name,
    String definitionSql
) {
}
