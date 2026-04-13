package at.jku.dke.task_app.sql_ddl_new.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link at.jku.dke.task_app.sql_ddl_new.data.entities.BinarySearchTask}
 *
 * @param solution The solution.
 */
public record BinarySearchTaskDto(@NotNull Integer solution) implements Serializable {
}
