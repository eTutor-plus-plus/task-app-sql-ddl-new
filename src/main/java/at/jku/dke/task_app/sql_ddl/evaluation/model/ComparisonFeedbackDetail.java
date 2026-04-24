package at.jku.dke.task_app.sql_ddl.evaluation.model;

import java.util.List;

/**
 * Contains lists of matching and mismatching schema entries.
 *
 * @param successfulEntries   The entries that matched.
 * @param unsuccessfulEntries The entries that did not match.
 */
public record ComparisonFeedbackDetail(
    List<String> successfulEntries,
    List<String> unsuccessfulEntries
) implements CriterionFeedbackDetail {
}
