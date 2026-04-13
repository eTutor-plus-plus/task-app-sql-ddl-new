package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask}
 *
 * @param solution The solution.
 */
public record SQLDDLTaskDto(
    @NotNull String solution,
    @NotNull Integer tablePoints,
    @NotNull Integer primaryKeyPoints,
    @NotNull Integer foreignKeyPoints,
    @NotNull Integer constraintPoints,
    String whitelist,
    String insertStatements
) implements Serializable {}
