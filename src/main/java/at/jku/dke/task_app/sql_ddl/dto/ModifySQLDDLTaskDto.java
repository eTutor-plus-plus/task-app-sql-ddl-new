package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying a SQL DDL task.
 *
 * @param solution The solution.
 */
public record ModifySQLDDLTaskDto(@NotNull Integer solution) implements Serializable {
}
