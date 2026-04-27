package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for one check-constraint input row from the frontend.
 *
 * @param definition The check constraint name.
 * @param successfulStatements The insert statements which are successfully inserted into the table used for checking.
 * @param unsuccessfulStatements The insert statements which are not successfully inserted into the table used for checking.
 */
public record SQLDDLCheckConstraintDto(
    @NotNull String definition,
    @NotNull String successfulStatements,
    @NotNull String unsuccessfulStatements
) implements Serializable {}
