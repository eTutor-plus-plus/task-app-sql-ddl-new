package at.jku.dke.task_app.sql_ddl.evaluation.model;

import java.math.BigDecimal;

/**
 * Summary counts for a criterion used for coarse feedback levels.
 *
 * @param key     The message key of the criterion.
 * @param passed  Whether the criterion passed overall.
 * @param matched The number of matched elements.
 * @param total   The total number of expected elements.
 */
public record CriterionCountSummary(
    String key,
    boolean passed,
    int matched,
    int total,
    BigDecimal points
) {
}
