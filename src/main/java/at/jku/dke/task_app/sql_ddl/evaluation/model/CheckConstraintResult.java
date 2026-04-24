package at.jku.dke.task_app.sql_ddl.evaluation.model;

/**
 * Represents the result of a single check constraint.
 *
 * @param name   The displayed constraint name.
 * @param passed Whether the constraint passed.
 */
public record CheckConstraintResult(
    String name,
    boolean passed
) {
}
