package at.jku.dke.task_app.sql_ddl.evaluation;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import at.jku.dke.task_app.sql_ddl.evaluation.feedback.EvaluationFeedbackService;
import at.jku.dke.task_app.sql_ddl.evaluation.model.BlockedBySyntaxFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CheckConstraintFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CheckConstraintResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.ComparisonFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CriterionCountSummary;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CriterionEvaluation;
import at.jku.dke.task_app.sql_ddl.evaluation.model.EvaluationExecutionResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.EvaluationResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.SyntaxFeedbackDetail;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service that evaluates submissions.
 */
@Service
public class EvaluationService {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final SQLDDLTaskRepository taskRepository;
    private final EvaluationDatabaseConnectionManager connectionManager;
    private final SchemaMetadataExtractor schemaMetadataExtractor;
    private final SchemaComparisonService schemaComparisonService;
    private final EvaluationFeedbackService feedbackService;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository          The task repository.
     * @param connectionManager       The database connection manager.
     * @param schemaMetadataExtractor The schema metadata extractor.
     * @param schemaComparisonService The schema comparison service.
     * @param feedbackService         The feedback service.
     */
    public EvaluationService(
        SQLDDLTaskRepository taskRepository,
        EvaluationDatabaseConnectionManager connectionManager,
        SchemaMetadataExtractor schemaMetadataExtractor,
        SchemaComparisonService schemaComparisonService,
        EvaluationFeedbackService feedbackService
    ) {
        this.taskRepository = taskRepository;
        this.connectionManager = connectionManager;
        this.schemaMetadataExtractor = schemaMetadataExtractor;
        this.schemaComparisonService = schemaComparisonService;
        this.feedbackService = feedbackService;
    }

    /**
     * Evaluates a input.
     *
     * @param submission The input to evaluate.
     * @return The evaluation result.
     */
    @Transactional
    public GradingDto evaluate(SubmitSubmissionDto<SQLDDLSubmissionDto> submission) {
        SQLDDLTask task = this.taskRepository.findById(submission.taskId())
            .orElseThrow(() -> new EntityNotFoundException("Task " + submission.taskId() + " does not exist."));

        LOG.info("Evaluating input for task {} with mode {} and feedback-level {}", submission.taskId(), submission.mode(), submission.feedbackLevel());
        EvaluationExecutionResult executionResult = executeSubmission(task, submission.submission().input());
        EvaluationResult evaluationResult = evaluateWithTask(task, executionResult);
        return feedbackService.toGrading(
            task,
            Locale.of(submission.language()),
            evaluationResult,
            submission.feedbackLevel(),
            submission.mode()
        );
    }

    EvaluationResult evaluateWithTask(SQLDDLTask task, EvaluationExecutionResult executionResult) {
        JsonNode expected = task.getExecutedSolution();
        List<CriterionEvaluation> criteria = new ArrayList<>();
        criteria.add(new CriterionEvaluation(
            "criterium.syntax",
            null,
            executionResult.syntaxValid(),
            new SyntaxFeedbackDetail(executionResult.errorMessage())
        ));

        if (!executionResult.syntaxValid()) {
            addBlockedCriterion(criteria, "criterium.tables");
            addBlockedCriterion(criteria, "criterium.primarykey");
            addBlockedCriterion(criteria, "criterium.foreignkey");
            addBlockedCriterion(criteria, "criterium.constraint");

            return new EvaluationResult(
                false,
                executionResult.errorMessage(),
                BigDecimal.ZERO,
                false,
                "incorrect",
                criteria,
                List.of(
                    new CriterionCountSummary("criterium.tables", false, 0, 0),
                    new CriterionCountSummary("criterium.primarykey", false, 0, 0),
                    new CriterionCountSummary("criterium.foreignkey", false, 0, 0),
                    new CriterionCountSummary("criterium.constraint", false, 0, 0)
                )
            );
        }

        JsonNode actual = executionResult.schemaMetadata();
        BigDecimal points = BigDecimal.ZERO;
        List<CriterionCountSummary> criterionCountSummaries = new ArrayList<>();

        boolean tablesMatch = schemaComparisonService.tablesMatch(expected, actual);
        points = points.add(addComparisonCriterion(
            criteria,
            "criterium.tables",
            tablesMatch,
            task.getTablePoints(),
            schemaComparisonService.matchingTableNames(expected, actual),
            schemaComparisonService.mismatchingTableNames(expected, actual)
        ));
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.tables",
            tablesMatch,
            schemaComparisonService.countMatchingTables(expected, actual),
            schemaComparisonService.countExpectedTables(expected)
        ));

        boolean primaryKeyMatch = schemaComparisonService.primaryKeysMatch(expected, actual);
        points = points.add(addComparisonCriterion(
            criteria,
            "criterium.primarykey",
            primaryKeyMatch,
            task.getPrimaryKeyPoints(),
            schemaComparisonService.matchingPrimaryKeyTableNames(expected, actual),
            schemaComparisonService.mismatchingPrimaryKeyTableNames(expected, actual)
        ));
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.primarykey",
            primaryKeyMatch,
            schemaComparisonService.countMatchingPrimaryKeys(expected, actual),
            schemaComparisonService.countExpectedPrimaryKeys(expected)
        ));

        boolean foreignKeyMatch = schemaComparisonService.foreignKeysMatch(expected, actual);
        points = points.add(addComparisonCriterion(
            criteria,
            "criterium.foreignkey",
            foreignKeyMatch,
            task.getForeignKeyPoints(),
            schemaComparisonService.matchingForeignKeyTableNames(expected, actual),
            schemaComparisonService.mismatchingForeignKeyTableNames(expected, actual)
        ));
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.foreignkey",
            foreignKeyMatch,
            schemaComparisonService.countMatchingForeignKeys(expected, actual),
            schemaComparisonService.countExpectedForeignKeys(expected)
        ));

        boolean uniqueMatch = schemaComparisonService.uniqueConstraintsMatch(expected, actual);
        boolean checkConstraintsMatch = executionResult.checkConstraintResults().stream().allMatch(CheckConstraintResult::passed);
        boolean constraintsMatch = uniqueMatch && checkConstraintsMatch;
        points = points.add(addConstraintCriterion(criteria, constraintsMatch, task.getConstraintPoints(), executionResult.checkConstraintResults()));
        int matchedConstraints = schemaComparisonService.countMatchingUniqueConstraints(expected, actual)
            + (int) executionResult.checkConstraintResults().stream().filter(CheckConstraintResult::passed).count();
        int expectedConstraints = schemaComparisonService.countExpectedUniqueConstraints(expected)
            + executionResult.checkConstraintResults().size();
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.constraint",
            constraintsMatch,
            matchedConstraints,
            expectedConstraints
        ));

        boolean solved = tablesMatch && primaryKeyMatch && foreignKeyMatch && constraintsMatch;
        return new EvaluationResult(
            true,
            null,
            points,
            solved,
            solved ? "correct" : "incorrect",
            criteria,
            criterionCountSummaries
        );
    }

    private void addBlockedCriterion(List<CriterionEvaluation> criteria, String criterionNameKey) {
        criteria.add(new CriterionEvaluation(
            criterionNameKey,
            BigDecimal.ZERO,
            false,
            new BlockedBySyntaxFeedbackDetail()
        ));
    }

    private BigDecimal addComparisonCriterion(
        List<CriterionEvaluation> criteria,
        String criterionNameKey,
        boolean passed,
        Integer points,
        List<String> successfulEntries,
        List<String> unsuccessfulEntries
    ) {
        BigDecimal awardedPoints = passed ? BigDecimal.valueOf(points) : BigDecimal.ZERO;
        criteria.add(new CriterionEvaluation(
            criterionNameKey,
            awardedPoints,
            passed,
            new ComparisonFeedbackDetail(successfulEntries, unsuccessfulEntries)
        ));
        return awardedPoints;
    }

    private BigDecimal addConstraintCriterion(
        List<CriterionEvaluation> criteria,
        boolean passed,
        Integer points,
        List<CheckConstraintResult> checkConstraintResults
    ) {
        BigDecimal awardedPoints = passed ? BigDecimal.valueOf(points) : BigDecimal.ZERO;
        criteria.add(new CriterionEvaluation(
            "criterium.constraint",
            awardedPoints,
            passed,
            new CheckConstraintFeedbackDetail(checkConstraintResults)
        ));
        return awardedPoints;
    }

    EvaluationExecutionResult executeSubmission(SQLDDLTask task, String ddl) {
        try (Connection connection = connectionManager.openForSubmission(task.getId())) {
            RunScript.execute(connection, new StringReader(ddl));
            JsonNode schemaMetadata = schemaMetadataExtractor.extract(connection, "PUBLIC");
            List<CheckConstraintResult> checkConstraintResults = evaluateCheckConstraints(task.getCheckConstraints(), connection);
            return new EvaluationExecutionResult(true, null, schemaMetadata, checkConstraintResults);
        } catch (SQLException ex) {
            LOG.info("DDL execution failed for task {}: {}", task.getId(), ex.getMessage());
            return new EvaluationExecutionResult(false, ex.getMessage(), null, List.of());
        }
    }

    private List<CheckConstraintResult> evaluateCheckConstraints(List<SQLDDLCheckConstraint> checkConstraints, Connection connection) throws SQLException {
        if (checkConstraints == null || checkConstraints.isEmpty()) {
            return List.of();
        }

        List<CheckConstraintResult> results = new ArrayList<>();
        for (SQLDDLCheckConstraint checkConstraint : checkConstraints) {
            boolean passed = evaluateCheckConstraint(checkConstraint, connection);
            String name = checkConstraint.getCheckDefinition() == null || checkConstraint.getCheckDefinition().isBlank()
                ? "<unnamed>"
                : checkConstraint.getCheckDefinition();
            results.add(new CheckConstraintResult(name, passed));
        }
        return results;
    }

    private boolean evaluateCheckConstraint(SQLDDLCheckConstraint checkConstraint, Connection connection) throws SQLException {
        var savepoint = connection.setSavepoint();
        try {
            if (!executeSuccessfulStatements(checkConstraint, connection)) {
                return false;
            }
            return executeUnsuccessfulStatements(checkConstraint, connection);
        } finally {
            connection.rollback(savepoint);
        }
    }

    private boolean executeSuccessfulStatements(SQLDDLCheckConstraint checkConstraint, Connection connection) {
        String statements = checkConstraint.getSuccessfulInsertStatements();
        if (statements == null || statements.isBlank()) {
            return true;
        }

        try {
            RunScript.execute(connection, new StringReader(statements));
            return true;
        } catch (SQLException ex) {
            LOG.info("Successful insert statements failed for '{}': {}", checkConstraint.getCheckDefinition(), ex.getMessage());
            return false;
        }
    }

    private boolean executeUnsuccessfulStatements(SQLDDLCheckConstraint checkConstraint, Connection connection) {
        String statements = checkConstraint.getUnsuccessfulInsertStatements();
        if (statements == null || statements.isBlank()) {
            return true;
        }

        try {
            RunScript.execute(connection, new StringReader(statements));
            LOG.info("Unsuccessful insert statements unexpectedly succeeded for '{}'", checkConstraint.getCheckDefinition());
            return false;
        } catch (SQLException ex) {
            if (!"23513".equals(ex.getSQLState())) {
                LOG.info(
                    "Unsuccessful insert statements failed for '{}' with unexpected SQL state {}: {}",
                    checkConstraint.getCheckDefinition(),
                    ex.getSQLState(),
                    ex.getMessage()
                );
                return false;
            }
            return true;
        }
    }
}
