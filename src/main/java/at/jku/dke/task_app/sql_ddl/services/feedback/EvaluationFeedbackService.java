package at.jku.dke.task_app.sql_ddl.services.feedback;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.AssertionFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.AssertionResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.BlockedBySyntaxFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.ConstraintFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.check.CheckConstraintResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.ComparisonFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.CriterionCountSummary;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.CriterionEvaluation;
import at.jku.dke.task_app.sql_ddl.evaluation.model.evaluation.EvaluationResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.feedback.SyntaxFeedbackDetail;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps internal evaluation results to localized grading responses.
 */
@Service
public class EvaluationFeedbackService {
    private static final String HTML_LINE_BREAK = "<br>";

    private final MessageSource messageSource;


    public EvaluationFeedbackService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public GradingDto toGrading(
        SQLDDLTask task,
        Locale locale,
        EvaluationResult evaluationResult,
        Integer requestedFeedbackLevel,
        SubmissionMode mode
    ) {
        return switch (mode) {
            case RUN -> createRunResult(task, locale, evaluationResult);
            case DIAGNOSE, SUBMIT -> applyFeedbackLevel(task, locale, evaluationResult, requestedFeedbackLevel);
        };
    }

    private GradingDto createRunResult(SQLDDLTask task, Locale locale, EvaluationResult evaluationResult) {
        CriterionEvaluation syntaxCriterion = getSyntaxCriterion(evaluationResult);
        List<CriterionDto> criteria = List.of(new CriterionDto(getMessage(syntaxCriterion.key(), locale), syntaxCriterion.awardedPoints(), syntaxCriterion.passed(), buildDetailedFeedback(locale, syntaxCriterion)));
        String feedback = buildGeneralFeedback(
            locale,
            evaluationResult.syntaxValid() ? "run.syntax.valid" : "run.syntax.invalid",
            evaluationResult.whitelistViolations()
        );
        return new GradingDto(task.getMaxPoints(), BigDecimal.ZERO, feedback, criteria);
    }

    private GradingDto applyFeedbackLevel(
        SQLDDLTask task,
        Locale locale,
        EvaluationResult evaluationResult,
        Integer requestedFeedbackLevel
    ) {
        if (requestedFeedbackLevel == null || requestedFeedbackLevel < 0 || requestedFeedbackLevel > 3) {
            throw new IllegalStateException("Unexpected Feedback value: " + requestedFeedbackLevel);
        }

        switch (requestedFeedbackLevel) {
            case 0:
                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    buildGeneralFeedback(locale, evaluationResult.generalFeedbackKey(), evaluationResult.whitelistViolations()),
                    List.of()
                );
            case 1:
                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    buildGeneralFeedback(locale, evaluationResult.generalFeedbackKey(), evaluationResult.whitelistViolations()),
                    evaluationResult.criteria().stream()
                        .map(criterion -> new CriterionDto(getMessage(criterion.key(), locale), criterion.awardedPoints(), criterion.passed(), ""))
                        .toList()
                );
            case 2:
                List<CriterionDto> resultWithFeedback = new ArrayList<>();
                CriterionEvaluation syntaxCriterion = getSyntaxCriterion(evaluationResult);
                resultWithFeedback.add(new CriterionDto(
                    getMessage(syntaxCriterion.key(), locale),
                    null,
                    syntaxCriterion.passed(),
                    getMessage("feedback.level2.criterion.summary", locale, syntaxCriterion.passed() ? 1 : 0, 1)
                ));

                for (CriterionCountSummary summary : evaluationResult.criterionCountSummaries()) {
                    resultWithFeedback.add(new CriterionDto(
                        getMessage(summary.key(), locale),
                        summary.points(),
                        summary.passed(),
                        getMessage("feedback.level2.criterion.summary", locale, summary.matched(), summary.total())
                    ));
                }

                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    buildGeneralFeedback(locale, evaluationResult.generalFeedbackKey(), evaluationResult.whitelistViolations()),
                    resultWithFeedback
                );
            case 3:
                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    buildGeneralFeedback(locale, evaluationResult.generalFeedbackKey(), evaluationResult.whitelistViolations()),
                    Stream.concat(
                            evaluationResult.criteria().stream().filter(CriterionEvaluation::passed),
                            evaluationResult.criteria().stream().filter(c -> !c.passed())
                        )
                        .map(criterion -> new CriterionDto(
                            getMessage(criterion.key(), locale),
                            criterion.awardedPoints(),
                            criterion.passed(),
                            buildDetailedFeedback(locale, criterion)
                        ))
                        .toList()
                );
            default:
                throw new IllegalStateException("Unexpected value: " + requestedFeedbackLevel);
        }
    }

    private String buildGeneralFeedback(Locale locale, String generalFeedbackKey, List<String> whitelistViolations) {
        String whitelistMessage = "";
        if (whitelistViolations != null && !whitelistViolations.isEmpty()) {
            whitelistMessage = getMessage("feedback.whitelist.invalid", locale, String.join(", ", whitelistViolations));
        }

        return getMessage(generalFeedbackKey, locale) + HTML_LINE_BREAK + whitelistMessage;
    }

    private String buildDetailedFeedback(Locale locale, CriterionEvaluation criterion) {
        switch (criterion.feedbackDetail()) {
            case SyntaxFeedbackDetail detail:
                return criterion.passed() ?
                    getMessage("criterium.syntax.valid", locale)
                    : getMessage("criterium.syntax.invalid", locale, detail.errorMessage());
            case BlockedBySyntaxFeedbackDetail ignored:
                return getMessage("criterium.blockedBySyntax", locale);
            case ComparisonFeedbackDetail detail:
                String successfulCriterion = detail.successfulEntries() == null ? "" : String.join(", ", detail.successfulEntries());
                String unsuccessfulCriterion = detail.unsuccessfulEntries() == null ? "" : String.join(", ", detail.unsuccessfulEntries());
                int successfulCriterionCount = detail.successfulEntries() == null ? 0 : detail.successfulEntries().size();
                int unsuccessfulCriterionCount = detail.unsuccessfulEntries() == null ? 0 : detail.unsuccessfulEntries().size();
                return buildSuccessFailureFeedback(
                    locale,
                    successfulCriterionCount,
                    successfulCriterion,
                    unsuccessfulCriterionCount,
                    unsuccessfulCriterion
                );
            case ConstraintFeedbackDetail detail:
                List<CheckConstraintResult> checkConstraintResults = detail.checkConstraintResults() == null
                    ? List.of()
                    : detail.checkConstraintResults();

                String successfulChecks = checkConstraintResults.stream()
                    .filter(CheckConstraintResult::passed)
                    .map(CheckConstraintResult::name)
                    .collect(Collectors.joining(", "));

                String unsuccessfulChecks = checkConstraintResults.stream()
                    .filter(result -> !result.passed())
                    .map(CheckConstraintResult::name)
                    .collect(Collectors.joining(", "));

                int totalChecks = checkConstraintResults.size();

                return buildConstraintFeedback(
                    locale,
                    detail.matchingUniqueConstraints(),
                    detail.expectedUniqueConstraints(),
                    successfulChecks,
                    unsuccessfulChecks,
                    totalChecks
                );
            case AssertionFeedbackDetail detail:
                List<AssertionResult> assertionResults = detail.assertionResults() == null
                    ? List.of()
                    : detail.assertionResults();

                String successfulAssertions = assertionResults.stream()
                    .filter(AssertionResult::passed)
                    .map(AssertionResult::name)
                    .collect(Collectors.joining(", "));

                String unsuccessfulAssertions = assertionResults.stream()
                    .filter(result -> !result.passed())
                    .map(AssertionResult::name)
                    .collect(Collectors.joining(", "));

                return buildAssertionFeedback(
                    locale,
                    successfulAssertions,
                    unsuccessfulAssertions,
                    assertionResults.size()
                );
            default:
                throw new IllegalStateException("Unexpected value: " + criterion.feedbackDetail());
        }
    }

    private String buildConstraintFeedback(
        Locale locale,
        int matchingUniqueConstraints,
        int expectedUniqueConstraints,
        String successfulChecks,
        String unsuccessfulChecks,
        int totalChecks
    ) {
        String successful = successfulChecks == null || successfulChecks.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : successfulChecks;
        String unsuccessful = unsuccessfulChecks == null || unsuccessfulChecks.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : unsuccessfulChecks;

        return getMessage("criterium.details.total", locale, totalChecks + expectedUniqueConstraints)
            + HTML_LINE_BREAK
            + getMessage("criterium.constraint.details.unique", locale, matchingUniqueConstraints, expectedUniqueConstraints)
            + HTML_LINE_BREAK
            + getMessage("criterium.constraint.details.check.successful", locale, successful)
            + HTML_LINE_BREAK
            + getMessage("criterium.constraint.details.check.unsuccessful", locale, unsuccessful);
    }

    private String buildSuccessFailureFeedback(
        Locale locale,
        int successfulCount,
        String successfulEntries,
        int unsuccessfulCount,
        String unsuccessfulEntries
    ) {
        String successful = successfulEntries == null || successfulEntries.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : successfulEntries;
        String unsuccessful = unsuccessfulEntries == null || unsuccessfulEntries.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : unsuccessfulEntries;

        return getMessage("criterium.details.total", locale, successfulCount + unsuccessfulCount)
            + HTML_LINE_BREAK
            + getMessage("criterium.details.successful", locale, successfulCount, successful)
            + HTML_LINE_BREAK
            + getMessage("criterium.details.unsuccessful", locale, unsuccessfulCount, unsuccessful);
    }

    private String buildAssertionFeedback(
        Locale locale,
        String successfulAssertions,
        String unsuccessfulAssertions,
        int totalAssertions
    ) {
        String successful = successfulAssertions == null || successfulAssertions.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : successfulAssertions;
        String unsuccessful = unsuccessfulAssertions == null || unsuccessfulAssertions.isBlank()
            ? getMessage("criterium.details.empty", locale)
            : unsuccessfulAssertions;

        return getMessage("criterium.details.total", locale, totalAssertions)
            + HTML_LINE_BREAK
            + getMessage("criterium.assertion.details.successful", locale, successful)
            + HTML_LINE_BREAK
            + getMessage("criterium.assertion.details.unsuccessful", locale, unsuccessful);
    }

    private CriterionEvaluation getSyntaxCriterion(EvaluationResult evaluationResult) {
        return evaluationResult.criteria().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Evaluation result does not contain a syntax criterion."));
    }

    private String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
