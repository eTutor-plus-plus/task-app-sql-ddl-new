package at.jku.dke.task_app.sql_ddl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Represents a SQL DDL task.
 */
@Entity
@Table(name = "task")
public class SQLDDLTask extends BaseTaskInGroup<SQLDDLTaskGroup> {
    @NotNull
    @Column(name = "solution", nullable = false)
    private Integer solution;

    /**
     * Creates a new instance of class {@link SQLDDLTask}.
     */
    public SQLDDLTask() {
    }

    /**
     * Creates a new instance of class {@link SQLDDLTask}.
     *
     * @param solution The solution.
     */
    public SQLDDLTask(Integer solution) {
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link SQLDDLTask}.
     *
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution.
     */
    public SQLDDLTask(BigDecimal maxPoints, TaskStatus status, SQLDDLTaskGroup taskGroup, Integer solution) {
        super(maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link SQLDDLTask}.
     *
     * @param id        The identifier.
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution.
     */
    public SQLDDLTask(Long id, BigDecimal maxPoints, TaskStatus status, SQLDDLTaskGroup taskGroup, Integer solution) {
        super(id, maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Gets the solution.
     *
     * @return The solution.
     */
    public Integer getSolution() {
        return solution;
    }

    /**
     * Sets the solution.
     *
     * @param solution The solution.
     */
    public void setSolution(Integer solution) {
        this.solution = solution;
    }
}
