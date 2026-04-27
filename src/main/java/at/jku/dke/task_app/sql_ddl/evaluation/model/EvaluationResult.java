package at.jku.dke.task_app.sql_ddl.evaluation.model;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @param syntaxValid             Whether the submission syntax was valid.
 * @param syntaxErrorMessage      The syntax error message, if any.
 * @param points                  The awarded points.
 * @param solved                  Whether the full task was solved.
 * @param generalFeedbackKey      Message key for the overall feedback.
 * @param whitelistViolations     Distinct submission words that are not part of the task whitelist.
 * @param criteria                Evaluated criteria in display order.
 * @param criterionCountSummaries Coarse summaries for feedback level 2.
 */
public record EvaluationResult(
    boolean syntaxValid,
    String syntaxErrorMessage,
    BigDecimal points,
    boolean solved,
    String generalFeedbackKey,
    List<String> whitelistViolations,
    List<CriterionEvaluation> criteria,
    List<CriterionCountSummary> criterionCountSummaries
) {
}
