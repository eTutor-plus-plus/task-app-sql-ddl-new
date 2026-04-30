package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * This class represents a data transfer object for modifying a SQL DDL task.
 *
 * @param solution The solution.
 */
public record ModifySQLDDLTaskDto(
    @NotNull String solution,
    @NotNull BigDecimal tablePoints,
    @NotNull BigDecimal primaryKeyPoints,
    @NotNull BigDecimal foreignKeyPoints,
    @NotNull BigDecimal constraintPoints,
    BigDecimal assertionPoints,
    String whitelist,
    List<SQLDDLCheckConstraintDto> insertStatements,
    List<SQLDDLAssertionDto> assertionStatements
) implements Serializable {}
