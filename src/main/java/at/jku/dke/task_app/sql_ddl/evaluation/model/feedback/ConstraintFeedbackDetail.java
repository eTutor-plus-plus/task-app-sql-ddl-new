package at.jku.dke.task_app.sql_ddl.evaluation.model.feedback;

import at.jku.dke.task_app.sql_ddl.evaluation.model.check.CheckConstraintResult;

import java.util.List;

/**
 * Contains constraint evaluation details for UNIQUE and CHECK constraints.
 *
 * @param matchingUniqueConstraints The number of matching UNIQUE constraints.
 * @param expectedUniqueConstraints The number of expected UNIQUE constraints.
 * @param checkConstraintResults    The evaluated CHECK constraint results.
 */
public record ConstraintFeedbackDetail(
    int matchingUniqueConstraints,
    int expectedUniqueConstraints,
    List<CheckConstraintResult> checkConstraintResults
) implements CriterionFeedbackDetail {
}
