package at.jku.dke.task_app.sql_ddl.evaluation.model.evaluation;

import at.jku.dke.task_app.sql_ddl.evaluation.model.check.CheckConstraintResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.AssertionResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Technical execution result of a submission.
 *
 * @param syntaxValid            Whether the DDL executed successfully.
 * @param errorMessage           The execution error message, if any.
 * @param schemaMetadata         The extracted schema metadata.
 * @param checkConstraintResults The evaluated check constraint results.
 * @param assertionResults       The evaluated assertion results.
 * @param assertionErrors        Assertion preprocessing or matching errors.
 */
public record EvaluationExecutionResult(
    boolean syntaxValid,
    String errorMessage,
    JsonNode schemaMetadata,
    List<CheckConstraintResult> checkConstraintResults,
    List<AssertionResult> assertionResults,
    List<String> assertionErrors
) {
}
