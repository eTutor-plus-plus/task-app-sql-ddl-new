package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTaskGroup}
 *
 * @param minNumber The minimum number.
 * @param maxNumber The maximum number.
 */
public record SQLDDLTaskGroupDto(@NotNull Integer minNumber, @NotNull Integer maxNumber) implements Serializable {
}
