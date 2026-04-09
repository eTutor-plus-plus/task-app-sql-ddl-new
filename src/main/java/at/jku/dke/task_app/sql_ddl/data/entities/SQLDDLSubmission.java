package at.jku.dke.task_app.sql_ddl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseSubmission;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a SQL DDL input.
 */
@Entity
@Table(name = "submission")
public class SQLDDLSubmission extends BaseSubmission<SQLDDLTask> {
    @NotNull
    @Column(name = "submission", nullable = false)
    private String submission;

    /**
     * Creates a new instance of class {@link SQLDDLSubmission}.
     */
    public SQLDDLSubmission() {
    }

    /**
     * Creates a new instance of class {@link SQLDDLSubmission}.
     *
     * @param submission The input.
     */
    public SQLDDLSubmission(String submission) {
        this.submission = submission;
    }

    /**
     * Creates a new instance of class {@link SQLDDLSubmission}.
     *
     * @param userId        The user id.
     * @param assignmentId  The assignment id.
     * @param task          The task.
     * @param language      The language.
     * @param feedbackLevel The feedback level.
     * @param mode          The mode.
     * @param submission    The input.
     */
    public SQLDDLSubmission(String userId, String assignmentId, SQLDDLTask task, String language, int feedbackLevel, SubmissionMode mode, String submission) {
        super(userId, assignmentId, task, language, feedbackLevel, mode);
        this.submission = submission;
    }

    /**
     * Gets the input.
     *
     * @return The input.
     */
    public String getSubmission() {
        return submission;
    }

    /**
     * Sets the input.
     *
     * @param submission The input.
     */
    public void setSubmission(String submission) {
        this.submission = submission;
    }
}
