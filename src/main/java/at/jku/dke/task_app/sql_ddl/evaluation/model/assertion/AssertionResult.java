package at.jku.dke.task_app.sql_ddl.evaluation.model.assertion;

/**
 * Represents the result of a single assertion.
 *
 * @param name   The displayed assertion definition.
 * @param passed Whether the assertion passed.
 */
public record AssertionResult(
    String name,
    boolean passed
) {
}
