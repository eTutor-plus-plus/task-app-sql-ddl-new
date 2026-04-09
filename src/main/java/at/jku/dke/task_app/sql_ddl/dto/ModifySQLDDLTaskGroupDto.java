package at.jku.dke.task_app.sql_ddl.dto;

import at.jku.dke.task_app.sql_ddl.validation.ValidTaskGroupNumber;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying a SQL DDL task group.
 *
 * @param minNumber The minimum number.
 * @param maxNumber The maximum number.
 */
@ValidTaskGroupNumber
public record ModifySQLDDLTaskGroupDto(@NotNull Integer minNumber, @NotNull Integer maxNumber) implements Serializable {
}
