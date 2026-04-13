package at.jku.dke.task_app.sql_ddl_new.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying a SQL DDL NEW task.
 *
 * @param solution The solution.
 */
public record ModifyBinarySearchTaskDto(@NotNull Integer solution) implements Serializable {
}
