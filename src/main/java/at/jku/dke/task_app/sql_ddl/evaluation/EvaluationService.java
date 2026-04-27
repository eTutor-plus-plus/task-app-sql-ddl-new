package at.jku.dke.task_app.sql_ddl.evaluation;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import at.jku.dke.task_app.sql_ddl.evaluation.feedback.EvaluationFeedbackService;
import at.jku.dke.task_app.sql_ddl.evaluation.feedback.WhitelistWordService;
import at.jku.dke.task_app.sql_ddl.evaluation.model.BlockedBySyntaxFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.ConstraintFeedbackDetail;
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
import java.math.RoundingMode;
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
    private static final int DIVISION_SCALE = 10;
    private static final int OUTPUT_SCALE = 2;

    private final SQLDDLTaskRepository taskRepository;
    private final EvaluationDatabaseConnectionManager connectionManager;
    private final SchemaMetadataExtractor schemaMetadataExtractor;
    private final SchemaComparisonService schemaComparisonService;
    private final WhitelistWordService whitelistWordService;
    private final EvaluationFeedbackService feedbackService;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository          The task repository.
     * @param connectionManager       The database connection manager.
     * @param schemaMetadataExtractor The schema metadata extractor.
     * @param schemaComparisonService The schema comparison service.
     * @param whitelistWordService    Service for whitelist comparisons.
     * @param feedbackService         The feedback service.
     */
    public EvaluationService(
        SQLDDLTaskRepository taskRepository,
        EvaluationDatabaseConnectionManager connectionManager,
        SchemaMetadataExtractor schemaMetadataExtractor,
        SchemaComparisonService schemaComparisonService,
        WhitelistWordService whitelistWordService,
        EvaluationFeedbackService feedbackService
    ) {
        this.taskRepository = taskRepository;
        this.connectionManager = connectionManager;
        this.schemaMetadataExtractor = schemaMetadataExtractor;
        this.schemaComparisonService = schemaComparisonService;
        this.whitelistWordService = whitelistWordService;
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
        EvaluationResult evaluationResult = evaluateWithTask(task, executionResult, submission.submission().input());
        return feedbackService.toGrading(
            task,
            Locale.of(submission.language()),
            evaluationResult,
            submission.feedbackLevel(),
            submission.mode()
        );
    }

    EvaluationResult evaluateWithTask(SQLDDLTask task, EvaluationExecutionResult executionResult, String submissionInput) {
        JsonNode expected = task.getExecutedSolution();
        List<String> whitelistViolations = whitelistWordService.findWhitelistViolations(task.getWhitelist(), submissionInput);
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
                roundPoints(BigDecimal.ZERO),
                false,
                "incorrect",
                whitelistViolations,
                criteria,
                List.of(
                    new CriterionCountSummary("criterium.tables", false, 0, 0, null),
                    new CriterionCountSummary("criterium.primarykey", false, 0, 0, null),
                    new CriterionCountSummary("criterium.foreignkey", false, 0, 0, null),
                    new CriterionCountSummary("criterium.constraint", false, 0, 0, null)
                )
            );
        }

        JsonNode actual = executionResult.schemaMetadata();
        BigDecimal points = BigDecimal.ZERO;
        List<CriterionCountSummary> criterionCountSummaries = new ArrayList<>();

        boolean tablesMatch = schemaComparisonService.tablesMatch(expected, actual);
        int matchingTables = schemaComparisonService.countMatchingTables(expected, actual);
        int expectedTables = schemaComparisonService.countExpectedTables(expected);
        BigDecimal tablePoints = addComparisonCriterion(
            criteria,
            "criterium.tables",
            tablesMatch,
            task.getTablePoints(),
            matchingTables,
            expectedTables,
            schemaComparisonService.matchingTableNames(expected, actual),
            schemaComparisonService.mismatchingTableNames(expected, actual)
        );
        points = points.add(tablePoints);
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.tables",
            tablesMatch,
            matchingTables,
            expectedTables,
            roundPoints(tablePoints)
        ));

        boolean primaryKeyMatch = schemaComparisonService.primaryKeysMatch(expected, actual);
        int matchingPrimaryKeys = schemaComparisonService.countMatchingPrimaryKeys(expected, actual);
        int expectedPrimaryKeys = schemaComparisonService.countExpectedPrimaryKeys(expected);
        BigDecimal primaryKeyPoints = addComparisonCriterion(
            criteria,
            "criterium.primarykey",
            primaryKeyMatch,
            task.getPrimaryKeyPoints(),
            matchingPrimaryKeys,
            expectedPrimaryKeys,
            schemaComparisonService.matchingPrimaryKeyTableNames(expected, actual),
            schemaComparisonService.mismatchingPrimaryKeyTableNames(expected, actual)
        );

        points = points.add(primaryKeyPoints);
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.primarykey",
            primaryKeyMatch,
            matchingPrimaryKeys,
            expectedPrimaryKeys,
            roundPoints(primaryKeyPoints)
        ));

        boolean foreignKeyMatch = schemaComparisonService.foreignKeysMatch(expected, actual);
        int matchingForeignKeys = schemaComparisonService.countMatchingForeignKeys(expected, actual);
        int expectedForeignKeys = schemaComparisonService.countExpectedForeignKeys(expected);
        BigDecimal foreignKeyPoints = addComparisonCriterion(
            criteria,
            "criterium.foreignkey",
            foreignKeyMatch,
            task.getForeignKeyPoints(),
            matchingForeignKeys,
            expectedForeignKeys,
            schemaComparisonService.matchingForeignKeyTableNames(expected, actual),
            schemaComparisonService.mismatchingForeignKeyTableNames(expected, actual)
        );
        points = points.add(foreignKeyPoints);
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.foreignkey",
            foreignKeyMatch,
            matchingForeignKeys,
            expectedForeignKeys,
            roundPoints(foreignKeyPoints)
        ));

        boolean uniqueMatch = schemaComparisonService.uniqueConstraintsMatch(expected, actual);
        int matchingUniqueConstraints = schemaComparisonService.countMatchingUniqueConstraints(expected, actual);
        int expectedUniqueConstraints = schemaComparisonService.countExpectedUniqueConstraints(expected);
        boolean checkConstraintsMatch = executionResult.checkConstraintResults().stream().allMatch(CheckConstraintResult::passed);
        int matchingCheckConstraints = (int) executionResult.checkConstraintResults().stream()
            .filter(CheckConstraintResult::passed)
            .count();
        int expectedCheckConstraints = executionResult.checkConstraintResults().size();
        boolean constraintsMatch = uniqueMatch && checkConstraintsMatch;
        int matchedConstraints = matchingUniqueConstraints + matchingCheckConstraints;
        int expectedConstraints = expectedUniqueConstraints + expectedCheckConstraints;
        BigDecimal constraintPoints = addConstraintCriterion(
            criteria,
            constraintsMatch,
            task.getConstraintPoints(),
            matchedConstraints,
            expectedConstraints,
            matchingUniqueConstraints,
            expectedUniqueConstraints,
            executionResult.checkConstraintResults()
        );
        points = points.add(constraintPoints);
        criterionCountSummaries.add(new CriterionCountSummary(
            "criterium.constraint",
            constraintsMatch,
            matchedConstraints,
            expectedConstraints,
            roundPoints(constraintPoints)
        ));

        boolean solved = tablesMatch && primaryKeyMatch && foreignKeyMatch && constraintsMatch;
        return new EvaluationResult(
            true,
            null,
            roundPoints(points),
            solved,
            solved ? "correct" : "incorrect",
            whitelistViolations,
            criteria,
            criterionCountSummaries
        );
    }

    private void addBlockedCriterion(List<CriterionEvaluation> criteria, String criterionNameKey) {
        criteria.add(new CriterionEvaluation(
            criterionNameKey,
            roundPoints(BigDecimal.ZERO),
            false,
            new BlockedBySyntaxFeedbackDetail()
        ));
    }

    private BigDecimal addComparisonCriterion(
        List<CriterionEvaluation> criteria,
        String criterionNameKey,
        boolean passed,
        Integer points,
        int matched,
        int expected,
        List<String> successfulEntries,
        List<String> unsuccessfulEntries
    ) {
        BigDecimal awardedPoints = calculateAwardedPoints(points, matched, expected, passed);
        criteria.add(new CriterionEvaluation(
            criterionNameKey,
            roundPoints(awardedPoints),
            passed,
            new ComparisonFeedbackDetail(successfulEntries, unsuccessfulEntries)
        ));
        return awardedPoints;
    }

    private BigDecimal addConstraintCriterion(
        List<CriterionEvaluation> criteria,
        boolean passed,
        Integer points,
        int matched,
        int expected,
        int matchingUniqueConstraints,
        int expectedUniqueConstraints,
        List<CheckConstraintResult> checkConstraintResults
    ) {
        BigDecimal awardedPoints = calculateAwardedPoints(points, matched, expected, passed);
        criteria.add(new CriterionEvaluation(
            "criterium.constraint",
            roundPoints(awardedPoints),
            passed,
            new ConstraintFeedbackDetail(
                matchingUniqueConstraints,
                expectedUniqueConstraints,
                checkConstraintResults
            )
        ));
        return awardedPoints;
    }

    private BigDecimal calculateAwardedPoints(Integer points, int matched, int expected, boolean passed) {
        if (points == null || points == 0) {
            return BigDecimal.ZERO;
        }

        if (expected == 0) {
            return passed ? BigDecimal.valueOf(points) : BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(points)
            .multiply(BigDecimal.valueOf(matched));
    }

    private BigDecimal roundPoints(BigDecimal points) {
        return points.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
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
            String name = checkConstraint.getDefinition() == null || checkConstraint.getDefinition().isBlank()
                ? "<unnamed>"
                : checkConstraint.getDefinition();
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
            LOG.info("Successful insert statements failed for '{}': {}", checkConstraint.getDefinition(), ex.getMessage());
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
            LOG.info("Unsuccessful insert statements unexpectedly succeeded for '{}'", checkConstraint.getDefinition());
            return false;
        } catch (SQLException ex) {
            if (!"23513".equals(ex.getSQLState())) {
                LOG.info(
                    "Unsuccessful insert statements failed for '{}' with unexpected SQL state {}: {}",
                    checkConstraint.getDefinition(),
                    ex.getSQLState(),
                    ex.getMessage()
                );
                return false;
            }
            return true;
        }
    }
}
