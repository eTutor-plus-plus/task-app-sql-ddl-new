package at.jku.dke.task_app.sql_ddl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask}
 *
 * @param solution The solution.
 */
public record SQLDDLTaskDto(
    @NotNull String solution,
    @NotNull BigDecimal tablePoints,
    @NotNull BigDecimal primaryKeyPoints,
    @NotNull BigDecimal foreignKeyPoints,
    @NotNull BigDecimal constraintPoints,
    BigDecimal assertionPoints,
    String whitelist,
    String generatedWhitelist,
    List<SQLDDLCheckConstraintDto> insertStatements,
    List<SQLDDLAssertionDto> assertionStatements
) implements Serializable {}
