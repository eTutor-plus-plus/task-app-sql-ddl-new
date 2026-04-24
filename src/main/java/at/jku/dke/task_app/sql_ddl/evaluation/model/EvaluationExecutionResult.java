package at.jku.dke.task_app.sql_ddl.evaluation.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Technical execution result of a submission.
 *
 * @param syntaxValid            Whether the DDL executed successfully.
 * @param errorMessage           The execution error message, if any.
 * @param schemaMetadata         The extracted schema metadata.
 * @param checkConstraintResults The evaluated check constraint results.
 */
public record EvaluationExecutionResult(
    boolean syntaxValid,
    String errorMessage,
    JsonNode schemaMetadata,
    List<CheckConstraintResult> checkConstraintResults
) {
}
