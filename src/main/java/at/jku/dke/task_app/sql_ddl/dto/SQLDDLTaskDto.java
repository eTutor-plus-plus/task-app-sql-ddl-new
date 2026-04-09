package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask}
 *
 * @param solution The solution.
 */
public record SQLDDLTaskDto(@NotNull Integer solution) implements Serializable {
}
