package at.jku.dke.task_app.sql_ddl.evaluation.model.check;

/**
 * Represents the result of a single check constraint.
 *
 * @param name   The displayed constraint definition.
 * @param passed Whether the constraint passed.
 */
public record CheckConstraintResult(
    String name,
    boolean passed
) {
}
