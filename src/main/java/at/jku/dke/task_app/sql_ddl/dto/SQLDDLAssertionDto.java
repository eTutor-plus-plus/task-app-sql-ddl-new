package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for one assertion fixture row from the frontend.
 *
 * @param definition The assertion definition used for matching against the solution.
 * @param successfulStatements Statements that must leave the assertion satisfied.
 * @param unsuccessfulStatements Statements that must violate the assertion.
 */
public record SQLDDLAssertionDto(
    @NotNull String definition,
    @NotNull String successfulStatements,
    @NotNull String unsuccessfulStatements
) implements Serializable {}
