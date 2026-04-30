package at.jku.dke.task_app.sql_ddl.evaluation.model.evaluation;

import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.ExtractedAssertion;

import java.util.List;

/**
 * Represents the assertion extracted from a provided sql-ddl script.
 *
 * @param sanitizedDdl   The executable ddl script without assertions.
 * @param assertions The assertions extracted from a ddl script.
 * @param errors The errors occurred when tried to extract the assertions from the ddl script.
 */
public record PreprocessingResult(
    String sanitizedDdl,
    List<ExtractedAssertion> assertions,
    List<String> errors
) {
}
