package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLSubmission;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import at.jku.dke.task_app.sql_ddl.evaluation.EvaluationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SQLDDLSubmissionServiceTest {

    @Test
    void createSubmissionEntity() {
        // Arrange
        SubmitSubmissionDto<SQLDDLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-quiz", 3L, "de", SubmissionMode.SUBMIT, 2, new SQLDDLSubmissionDto("33"));
        SQLDDLSubmissionService service = new SQLDDLSubmissionService(null, null, null);

        // Act
        SQLDDLSubmission submission = service.createSubmissionEntity(dto);

        // Assert
        assertEquals(dto.submission().input(), submission.getSubmission());
    }

    @Test
    void mapSubmissionToSubmissionData() {
        // Arrange
        SQLDDLSubmission submission = new SQLDDLSubmission("33");
        SQLDDLSubmissionService service = new SQLDDLSubmissionService(null, null, null);

        // Act
        SQLDDLSubmissionDto dto = service.mapSubmissionToSubmissionData(submission);

        // Assert
        assertEquals(submission.getSubmission(), dto.input());
    }

    @Test
    void evaluate() {
        // Arrange
        var evalService = mock(EvaluationService.class);
        SubmitSubmissionDto<SQLDDLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-quiz", 3L, "de", SubmissionMode.SUBMIT, 2, new SQLDDLSubmissionDto("33"));
        SQLDDLSubmissionService service = new SQLDDLSubmissionService(null, null, evalService);

        // Act
        var result = service.evaluate(dto);

        // Assert
        verify(evalService).evaluate(dto);
    }

}
