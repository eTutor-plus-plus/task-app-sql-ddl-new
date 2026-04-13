package at.jku.dke.task_app.sql_ddl.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Represents a check constraint belonging to a SQL DDL task.
 */
@Entity
@Table(name = "check_constraint")
public class SQLDDLCheckConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "check_condition")
    private String checkCondition;

    @Column(name = "number_successful_statements")
    private Integer numberSuccessfulStatements;

    @Column(name = "number_unsuccessful_statements")
    private Integer numberUnsuccessfulStatements;

    @Column(name = "insert_statements", columnDefinition = "text")
    private String insertStatements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private SQLDDLTask task;


    public SQLDDLCheckConstraint() {
    }

    public SQLDDLCheckConstraint(
        String checkCondition,
        Integer numberSuccessfulStatements,
        Integer numberUnsuccessfulStatements,
        String insertStatements
    ) {
        this.checkCondition = checkCondition;
        this.numberSuccessfulStatements = numberSuccessfulStatements;
        this.numberUnsuccessfulStatements = numberUnsuccessfulStatements;
        this.insertStatements = insertStatements;
    }

    public UUID getId() {
        return id;
    }

    public String getCheckCondition() {
        return checkCondition;
    }

    public void setCheckCondition(String checkCondition) {
        this.checkCondition = checkCondition;
    }

    public Integer getNumberSuccessfulStatements() {
        return numberSuccessfulStatements;
    }

    public void setNumberSuccessfulStatements(Integer numberSuccessfulStatements) {
        this.numberSuccessfulStatements = numberSuccessfulStatements;
    }

    public Integer getNumberUnsuccessfulStatements() {
        return numberUnsuccessfulStatements;
    }

    public void setNumberUnsuccessfulStatements(Integer numberUnsuccessfulStatements) {
        this.numberUnsuccessfulStatements = numberUnsuccessfulStatements;
    }

    public String getInsertStatements() {
        return insertStatements;
    }

    public void setInsertStatements(String insertStatements) {
        this.insertStatements = insertStatements;
    }

    public SQLDDLTask getTask() {
        return task;
    }

    public void setTask(SQLDDLTask task) {
        this.task = task;
    }
}
