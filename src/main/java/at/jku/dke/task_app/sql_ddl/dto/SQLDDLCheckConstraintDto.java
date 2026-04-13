package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for one check-constraint input row from the frontend.
 *
 * @param name The check constraint name/condition.
 * @param insertStatements The insert statements used for checking.
 */
public record SQLDDLCheckConstraintDto(
    @NotNull String name,
    @NotNull String insertStatements
) implements Serializable {}
