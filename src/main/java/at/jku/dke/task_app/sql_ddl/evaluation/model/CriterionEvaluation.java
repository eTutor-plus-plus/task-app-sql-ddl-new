package at.jku.dke.task_app.sql_ddl.evaluation.model;

import java.math.BigDecimal;

/**
 * Internal representation of an evaluated criterion.
 *
 * @param key            The message key of the criterion.
 * @param awardedPoints  The awarded points, if applicable.
 * @param passed         Whether the criterion passed.
 * @param feedbackDetail The structured feedback detail.
 */
public record CriterionEvaluation(
    String key,
    BigDecimal awardedPoints,
    boolean passed,
    CriterionFeedbackDetail feedbackDetail
) {
}
