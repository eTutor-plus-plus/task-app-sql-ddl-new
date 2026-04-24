package at.jku.dke.task_app.sql_ddl.evaluation.model;

/**
 *
 * @param errorMessage The execution error message, if any.
 */
public record SyntaxFeedbackDetail(String errorMessage) implements CriterionFeedbackDetail {
}
