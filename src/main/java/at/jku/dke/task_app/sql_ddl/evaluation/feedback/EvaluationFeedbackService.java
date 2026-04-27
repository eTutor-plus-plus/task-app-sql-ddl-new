package at.jku.dke.task_app.sql_ddl.evaluation.feedback;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.evaluation.model.BlockedBySyntaxFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CheckConstraintFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CheckConstraintResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.ComparisonFeedbackDetail;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CriterionCountSummary;
import at.jku.dke.task_app.sql_ddl.evaluation.model.CriterionEvaluation;
import at.jku.dke.task_app.sql_ddl.evaluation.model.EvaluationResult;
import at.jku.dke.task_app.sql_ddl.evaluation.model.SyntaxFeedbackDetail;
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
        String feedback = getMessage(
            evaluationResult.syntaxValid() ? "run.syntax.valid" : "run.syntax.invalid",
            locale
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
                    getMessage(evaluationResult.generalFeedbackKey(), locale),
                    List.of()
                );
            case 1:
                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    getMessage(evaluationResult.generalFeedbackKey(), locale),
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
                    getMessage(evaluationResult.generalFeedbackKey(), locale),
                    resultWithFeedback
                );
            case 3:
                return new GradingDto(
                    task.getMaxPoints(),
                    evaluationResult.points(),
                    getMessage(evaluationResult.generalFeedbackKey(), locale),
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
            case CheckConstraintFeedbackDetail detail:
                if (detail.checkConstraintResults() == null || detail.checkConstraintResults().isEmpty()) {
                    return getMessage("criterium.details.empty", locale);
                }

                String successfulChecks = detail.checkConstraintResults().stream()
                    .filter(CheckConstraintResult::passed)
                    .map(CheckConstraintResult::name)
                    .collect(Collectors.joining(", "));

                String unsuccessfulChecks = detail.checkConstraintResults().stream()
                    .filter(result -> !result.passed())
                    .map(CheckConstraintResult::name)
                    .collect(Collectors.joining(", "));

                int successfulCheckCount = (int) detail.checkConstraintResults().stream()
                    .filter(CheckConstraintResult::passed)
                    .count();
                int unsuccessfulCheckCount = (int) detail.checkConstraintResults().stream()
                    .filter(result -> !result.passed())
                    .count();

                return buildSuccessFailureFeedback(
                    locale,
                    successfulCheckCount,
                    successfulChecks,
                    unsuccessfulCheckCount,
                    unsuccessfulChecks
                );
            default:
                throw new IllegalStateException("Unexpected value: " + criterion.feedbackDetail());
        }
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

    private CriterionEvaluation getSyntaxCriterion(EvaluationResult evaluationResult) {
        return evaluationResult.criteria().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Evaluation result does not contain a syntax criterion."));
    }

    private String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
