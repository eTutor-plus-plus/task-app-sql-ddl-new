package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;

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
    Integer assertionPoints,
    String whitelist,
    List<SQLDDLCheckConstraintDto> insertStatements,
    List<SQLDDLCheckConstraintDto> assertionStatements
) implements Serializable {}
