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
 * Represents an assertion belonging to a SQL DDL task.
 */
@Entity
@Table(name = "assertion")
public class SQLDDLAssertion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "successful_statements", columnDefinition = "text")
    private String successfulStatements;

    @Column(name = "unsuccessful_statements", columnDefinition = "text")
    private String unsuccessfulStatements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private SQLDDLTask task;


    public SQLDDLAssertion() {
    }

    public SQLDDLAssertion(
        String name,
        String successfulStatements,
        String unsuccessfulStatements
    ) {
        this.name = name;
        this.successfulStatements = successfulStatements;
        this.unsuccessfulStatements = unsuccessfulStatements;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSuccessfulStatements() {
        return successfulStatements;
    }

    public void setSuccessfulStatements(String successfulStatements) {
        this.successfulStatements = successfulStatements;
    }

    public String getUnsuccessfulStatements() {
        return unsuccessfulStatements;
    }

    public void setUnsuccessfulStatements(String unsuccessfulStatements) {
        this.unsuccessfulStatements = unsuccessfulStatements;
    }

    public SQLDDLTask getTask() {
        return task;
    }

    public void setTask(SQLDDLTask task) {
        this.task = task;
    }
}
