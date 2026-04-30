package at.jku.dke.task_app.sql_ddl.evaluation.model.assertion;

import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.CriterionFeedbackDetail;

import java.util.List;

/**
 * Contains assertion evaluation details.
 *
 * @param assertionResults The evaluated assertions.
 * @param preprocessingErrors Assertion preprocessing errors found in the submission.
 */
public record AssertionFeedbackDetail(
    List<AssertionResult> assertionResults,
    List<String> preprocessingErrors
) implements CriterionFeedbackDetail {
}
