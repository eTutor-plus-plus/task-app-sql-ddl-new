package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.services.BaseSubmissionService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLSubmission;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLSubmissionRepository;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import at.jku.dke.task_app.sql_ddl.evaluation.EvaluationService;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for managing {@link SQLDDLSubmission}s.
 */
@Service
public class SQLDDLSubmissionService extends BaseSubmissionService<SQLDDLTask, SQLDDLSubmission, SQLDDLSubmissionDto> {

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of class {@link SQLDDLSubmissionService}.
     *
     * @param submissionRepository The input repository.
     * @param taskRepository       The task repository.
     * @param evaluationService    The evaluation service.
     */
    public SQLDDLSubmissionService(SQLDDLSubmissionRepository submissionRepository, SQLDDLTaskRepository taskRepository, EvaluationService evaluationService) {
        super(submissionRepository, taskRepository);
        this.evaluationService = evaluationService;
    }

    @Override
    protected SQLDDLSubmission createSubmissionEntity(SubmitSubmissionDto<SQLDDLSubmissionDto> submitSubmissionDto) {
        return new SQLDDLSubmission(submitSubmissionDto.submission().input());
    }

    @Override
    protected GradingDto evaluate(SubmitSubmissionDto<SQLDDLSubmissionDto> submitSubmissionDto) {
        return this.evaluationService.evaluate(submitSubmissionDto);
    }

    @Override
    protected SQLDDLSubmissionDto mapSubmissionToSubmissionData(SQLDDLSubmission submission) {
        return new SQLDDLSubmissionDto(submission.getSubmission());
    }

}
