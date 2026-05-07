package at.jku.dke.task_app.sql_ddl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTask;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "task")
public class SQLDDLTask extends BaseTask {
    @NotNull
    @Column(name = "solution", nullable = false, columnDefinition = "text")
    private String solution;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "executed_solution", nullable = false, columnDefinition = "jsonb")
    private JsonNode executedSolution;

    @NotNull
    @Column(name = "table_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal tablePoints;

    @NotNull
    @Column(name = "primarykey_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal primaryKeyPoints;

    @NotNull
    @Column(name = "foreignkey_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal foreignKeyPoints;

    @NotNull
    @Column(name = "constraint_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal constraintPoints;

    @NotNull
    @Column(name = "assertion_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal assertionPoints;

    @Column(name = "whitelist", columnDefinition = "text")
    private String whitelist;

    @NotNull
    @Column(name = "generated_whitelist", nullable = false, columnDefinition = "text")
    private String generatedWhitelist;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SQLDDLCheckConstraint> checkConstraints = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SQLDDLAssertion> assertions = new ArrayList<>();


    public SQLDDLTask() {
    }

    public SQLDDLTask(
        Long id,
        BigDecimal maxPoints,
        TaskStatus status,
        String solution,
        JsonNode executedSolution,
        BigDecimal tablePoints,
        BigDecimal primaryKeyPoints,
        BigDecimal foreignKeyPoints,
        BigDecimal constraintPoints,
        BigDecimal assertionPoints,
        String whitelist,
        String generatedWhitelist,
        List<SQLDDLCheckConstraint> checkConstraints,
        List<SQLDDLAssertion> assertions
    ) {
        super(id, maxPoints, status);
        this.solution = solution;
        this.executedSolution = executedSolution;
        this.tablePoints = tablePoints;
        this.primaryKeyPoints = primaryKeyPoints;
        this.foreignKeyPoints = foreignKeyPoints;
        this.constraintPoints = constraintPoints;
        this.assertionPoints = assertionPoints;
        this.whitelist = whitelist;
        this.generatedWhitelist = generatedWhitelist;
        this.checkConstraints = checkConstraints;
        this.assertions = assertions;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public JsonNode getExecutedSolution() {
        return executedSolution;
    }

    public void setExecutedSolution(JsonNode executedSolution) {
        this.executedSolution = executedSolution;
    }

    public BigDecimal getTablePoints() {
        return tablePoints;
    }

    public void setTablePoints(BigDecimal tablePoints) {
        this.tablePoints = tablePoints;
    }

    public BigDecimal getPrimaryKeyPoints() {
        return primaryKeyPoints;
    }

    public void setPrimaryKeyPoints(BigDecimal primaryKeyPoints) {
        this.primaryKeyPoints = primaryKeyPoints;
    }

    public BigDecimal getForeignKeyPoints() {
        return foreignKeyPoints;
    }

    public void setForeignKeyPoints(BigDecimal foreignKeyPoints) {
        this.foreignKeyPoints = foreignKeyPoints;
    }

    public BigDecimal getConstraintPoints() {
        return constraintPoints;
    }

    public void setConstraintPoints(BigDecimal constraintPoints) {
        this.constraintPoints = constraintPoints;
    }

    public BigDecimal getAssertionPoints() {
        return assertionPoints;
    }

    public void setAssertionPoints(BigDecimal assertionPoints) {
        this.assertionPoints = assertionPoints;
    }

    public String getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(String whitelist) {
        this.whitelist = whitelist;
    }

    public String getGeneratedWhitelist() {
        return generatedWhitelist;
    }

    public void setGeneratedWhitelist(String generatedWhitelist) {
        this.generatedWhitelist = generatedWhitelist;
    }

    public List<SQLDDLCheckConstraint> getCheckConstraints() {
        return checkConstraints;
    }

    public void setCheckConstraints(List<SQLDDLCheckConstraint> checkConstraints) {
        this.checkConstraints = checkConstraints;
    }

    public List<SQLDDLAssertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<SQLDDLAssertion> assertions) {
        this.assertions = assertions;
    }
}
