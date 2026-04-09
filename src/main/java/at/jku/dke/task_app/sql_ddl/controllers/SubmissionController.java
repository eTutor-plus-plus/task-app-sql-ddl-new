package at.jku.dke.task_app.sql_ddl.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLSubmission;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import at.jku.dke.task_app.sql_ddl.services.SQLDDLSubmissionService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link SQLDDLSubmission}s.
 */
@RestController
public class SubmissionController extends BaseSubmissionController<SQLDDLSubmissionDto> {
    /**
     * Creates a new instance of class {@link SubmissionController}.
     *
     * @param submissionService The input service.
     */
    public SubmissionController(SQLDDLSubmissionService submissionService) {
        super(submissionService);
    }
}
