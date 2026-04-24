package at.jku.dke.task_app.sql_ddl.evaluation.model;

import java.util.List;

/**
 * Contains check constraint evaluation details.
 *
 * @param checkConstraintResults The evaluated check constraint results.
 */
public record CheckConstraintFeedbackDetail(
    List<CheckConstraintResult> checkConstraintResults
) implements CriterionFeedbackDetail {
}
