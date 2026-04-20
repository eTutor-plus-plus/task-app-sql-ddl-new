package at.jku.dke.task_app.sql_ddl.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLCheckConstraint;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLSubmissionDto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service that evaluates submissions.
 */
@Service
public class EvaluationService {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final SQLDDLTaskRepository taskRepository;
    private final MessageSource messageSource;
    private final EvaluationDatabaseConnectionManager connectionManager;
    private final SchemaMetadataExtractor schemaMetadataExtractor;
    private final SchemaComparisonService schemaComparisonService;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository          The task repository.
     * @param messageSource           The message source.
     * @param connectionManager       The database connection manager.
     * @param schemaMetadataExtractor The schema metadata extractor.
     * @param schemaComparisonService The schema comparison service.
     */
    public EvaluationService(
        SQLDDLTaskRepository taskRepository,
        MessageSource messageSource,
        EvaluationDatabaseConnectionManager connectionManager,
        SchemaMetadataExtractor schemaMetadataExtractor,
        SchemaComparisonService schemaComparisonService
    ) {
        this.taskRepository = taskRepository;
        this.messageSource = messageSource;
        this.connectionManager = connectionManager;
        this.schemaMetadataExtractor = schemaMetadataExtractor;
        this.schemaComparisonService = schemaComparisonService;
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
        Locale locale = Locale.of(submission.language());
        List<CriterionDto> criteria = new ArrayList<>();
        EvaluationExecutionResult executionResult = executeSubmission(task, submission.submission().input());

        return switch (submission.mode()) {
            case RUN -> createRunResult(task, locale, criteria, executionResult);
            case DIAGNOSE, SUBMIT -> createResult(task, locale, criteria, executionResult);
            default -> throw new IllegalStateException("Unexpected value: " + submission.mode());
        };
    }

    private GradingDto createRunResult(
        SQLDDLTask task,
        Locale locale,
        List<CriterionDto> criteria,
        EvaluationExecutionResult executionResult
    ) {
        criteria.add(new CriterionDto(
            messageSource.getMessage("criterium.syntax", null, locale),
            null,
            executionResult.syntaxValid(),
            executionResult.syntaxValid()
                ? messageSource.getMessage("criterium.syntax.valid", null, locale)
                : messageSource.getMessage("criterium.syntax.invalid", new Object[]{executionResult.errorMessage()}, locale)));

        String feedback = messageSource.getMessage(
            executionResult.syntaxValid() ? "run.syntax.valid" : "run.syntax.invalid",
            null,
            locale
        );
        return new GradingDto(task.getMaxPoints(), BigDecimal.ZERO, feedback, criteria);
    }

    private GradingDto createResult(
        SQLDDLTask task,
        Locale locale,
        List<CriterionDto> criteria,
        EvaluationExecutionResult executionResult
    ) {
        criteria.add(new CriterionDto(
            messageSource.getMessage("criterium.syntax", null, locale),
            null,
            executionResult.syntaxValid(),
            executionResult.syntaxValid()
                ? messageSource.getMessage("criterium.syntax.valid", null, locale)
                : messageSource.getMessage("criterium.syntax.invalid", new Object[]{executionResult.errorMessage()}, locale)));

        if (!executionResult.syntaxValid()) {
            addBlockedCriterion(criteria, locale, "criterium.tables");
            addBlockedCriterion(criteria, locale, "criterium.primarykey");
            addBlockedCriterion(criteria, locale, "criterium.foreignkey");
            addBlockedCriterion(criteria, locale, "criterium.constraint");
            return new GradingDto(task.getMaxPoints(), BigDecimal.ZERO, messageSource.getMessage("incorrect", null, locale), criteria);
        }

        JsonNode expected = task.getExecutedSolution();
        JsonNode actual = executionResult.schemaMetadata();
        BigDecimal points = BigDecimal.ZERO;

        boolean tablesMatch = schemaComparisonService.tablesMatch(expected, actual);
        points = points.add(addComparisonCriterion(criteria, locale, "criterium.tables", "criterium.tables.match", "criterium.tables.mismatch", tablesMatch, task.getTablePoints()));

        boolean primaryKeyMatch = schemaComparisonService.primaryKeysMatch(expected, actual);
        points = points.add(addComparisonCriterion(criteria, locale, "criterium.primarykey", "criterium.primarykey.match", "criterium.primarykey.mismatch", primaryKeyMatch, task.getPrimaryKeyPoints()));

        boolean foreignKeyMatch = schemaComparisonService.foreignKeysMatch(expected, actual);
        points = points.add(addComparisonCriterion(criteria, locale, "criterium.foreignkey", "criterium.foreignkey.match", "criterium.foreignkey.mismatch", foreignKeyMatch, task.getForeignKeyPoints()));

        boolean uniqueMatch = schemaComparisonService.uniqueConstraintsMatch(expected, actual);
        boolean checkConstraintsMatch = executionResult.checkConstraintResults().stream().allMatch(CheckConstraintResult::passed);
        boolean constraintsMatch = uniqueMatch && checkConstraintsMatch;
        points = points.add(addConstraintCriterion(criteria, locale, constraintsMatch, task.getConstraintPoints(), executionResult.checkConstraintResults()));

        boolean solved = tablesMatch && primaryKeyMatch && foreignKeyMatch && constraintsMatch;
        String feedback = messageSource.getMessage(solved ? "correct" : "incorrect", null, locale);
        return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
    }

    private void addBlockedCriterion(List<CriterionDto> criteria, Locale locale, String criterionNameKey) {
        criteria.add(new CriterionDto(
            messageSource.getMessage(criterionNameKey, null, locale),
            BigDecimal.ZERO,
            false,
            messageSource.getMessage("criterium.blockedBySyntax", null, locale)
        ));
    }

    private BigDecimal addComparisonCriterion(
        List<CriterionDto> criteria,
        Locale locale,
        String criterionNameKey,
        String passedFeedbackKey,
        String failedFeedbackKey,
        boolean passed,
        Integer points
    ) {
        BigDecimal awardedPoints = passed ? BigDecimal.valueOf(points) : BigDecimal.ZERO;
        criteria.add(new CriterionDto(
            messageSource.getMessage(criterionNameKey, null, locale),
            awardedPoints,
            passed,
            messageSource.getMessage(passed ? passedFeedbackKey : failedFeedbackKey, null, locale)
        ));
        return awardedPoints;
    }

    private BigDecimal addConstraintCriterion(
        List<CriterionDto> criteria,
        Locale locale,
        boolean passed,
        Integer points,
        List<CheckConstraintResult> checkConstraintResults
    ) {
        BigDecimal awardedPoints = passed ? BigDecimal.valueOf(points) : BigDecimal.ZERO;
        String baseFeedback = messageSource.getMessage(
            passed ? "criterium.constraint.match" : "criterium.constraint.mismatch",
            null,
            locale
        );
        String feedback = buildCheckConstraintFeedback(locale, baseFeedback, checkConstraintResults);

        criteria.add(new CriterionDto(
            messageSource.getMessage("criterium.constraint", null, locale),
            awardedPoints,
            passed,
            feedback
        ));
        return awardedPoints;
    }

    private EvaluationExecutionResult executeSubmission(SQLDDLTask task, String ddl) {
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

    private String buildCheckConstraintFeedback(
        Locale locale,
        String baseFeedback,
        List<CheckConstraintResult> checkConstraintResults
    ) {
        if (checkConstraintResults == null || checkConstraintResults.isEmpty()) {
            String noChecks = messageSource.getMessage("criterium.constraint.details.none", null, locale);
            return baseFeedback + " " + noChecks;
        }

        String successful = checkConstraintResults.stream()
            .filter(CheckConstraintResult::passed)
            .map(CheckConstraintResult::name)
            .collect(Collectors.joining(", "));

        String unsuccessful = checkConstraintResults.stream()
            .filter(result -> !result.passed())
            .map(CheckConstraintResult::name)
            .collect(Collectors.joining(", "));

        if (successful.isBlank()) {
            successful = messageSource.getMessage("criterium.constraint.details.empty", null, locale);
        }
        if (unsuccessful.isBlank()) {
            unsuccessful = messageSource.getMessage("criterium.constraint.details.empty", null, locale);
        }

        String successfulText = messageSource.getMessage(
            "criterium.constraint.details.successful",
            new Object[]{successful},
            locale
        );
        String unsuccessfulText = messageSource.getMessage(
            "criterium.constraint.details.unsuccessful",
            new Object[]{unsuccessful},
            locale
        );
        return baseFeedback + " " + successfulText + " " + unsuccessfulText;
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

    private record EvaluationExecutionResult(
        boolean syntaxValid,
        String errorMessage,
        JsonNode schemaMetadata,
        List<CheckConstraintResult> checkConstraintResults
    ) {
    }

    private record CheckConstraintResult(
        String name,
        boolean passed
    ) {
    }
}
