package at.jku.dke.task_app.sql_ddl.evaluation.model.feedback;

import java.util.List;

/**
 * Contains lists of matching and mismatching schema entries.
 *
 * @param successfulEntries   The entries that matched.
 * @param unsuccessfulEntries The entries that did not match.
 * @param totalEntries        The number of expected entries that can contribute points.
 */
public record ComparisonFeedbackDetail(
    List<String> successfulEntries,
    List<String> unsuccessfulEntries,
    int totalEntries
) implements CriterionFeedbackDetail {
}
