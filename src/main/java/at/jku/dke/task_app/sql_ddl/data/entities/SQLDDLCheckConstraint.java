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

    @Column(name = "check_definition")
    private String checkDefinition;

    @Column(name = "successful_insert_statements")
    private String successfulInsertStatements;

    @Column(name = "unsuccessful_insert_statements", columnDefinition = "text")
    private String unsuccessfulInsertStatements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private SQLDDLTask task;


    public SQLDDLCheckConstraint() {
    }

    public SQLDDLCheckConstraint(
        String checkDefinition,
        String successfulInsertStatements,
        String unsuccessfulInsertStatements
    ) {
        this.checkDefinition = checkDefinition;
        this.successfulInsertStatements = successfulInsertStatements;
        this.unsuccessfulInsertStatements = unsuccessfulInsertStatements;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCheckDefinition() {
        return checkDefinition;
    }

    public void setCheckDefinition(String checkDefinition) {
        this.checkDefinition = checkDefinition;
    }

    public String getSuccessfulInsertStatements() {
        return successfulInsertStatements;
    }

    public void setSuccessfulInsertStatements(String successfulInsertStatements) {
        this.successfulInsertStatements = successfulInsertStatements;
    }

    public String getUnsuccessfulInsertStatements() {
        return unsuccessfulInsertStatements;
    }

    public void setUnsuccessfulInsertStatements(String unsuccessfulInsertStatements) {
        this.unsuccessfulInsertStatements = unsuccessfulInsertStatements;
    }

    public SQLDDLTask getTask() {
        return task;
    }

    public void setTask(SQLDDLTask task) {
        this.task = task;
    }
}
