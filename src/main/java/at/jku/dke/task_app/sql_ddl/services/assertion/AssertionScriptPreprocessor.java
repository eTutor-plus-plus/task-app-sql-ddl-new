package at.jku.dke.task_app.sql_ddl.services.assertion;

import at.jku.dke.task_app.sql_ddl.evaluation.model.assertion.ExtractedAssertion;
import at.jku.dke.task_app.sql_ddl.evaluation.model.evaluation.PreprocessingResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preprocesses a mixed DDL script before evaluation.
 * The preprocessor separates supported {@code CREATE ASSERTION ... CHECK (...)} statements from the
 * remaining DDL, because assertions are handled by the application while the sanitized DDL is executed
 * directly against H2. During this pass it also reports unsupported assertion syntax and duplicate
 * assertion names.
 * NOTE: This class was created by AI.
 */
@Service
public class AssertionScriptPreprocessor {
    /**
     * Matches exactly the supported assertion syntax
     * {@code CREATE ASSERTION <name> CHECK (<condition>)}.
     *
     * <p>The pattern is case-insensitive, spans multiple lines, captures the assertion identifier in
     * group 1, and captures the raw {@code CHECK} condition in group 2.</p>
     */
    private static final Pattern SUPPORTED_ASSERTION_PATTERN = Pattern.compile(
        "(?is)^CREATE\\s+ASSERTION\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+CHECK\\s*\\((.*)\\)\\s*;\\s*$"
    );

    /**
     * Detects statements that begin with {@code CREATE ASSERTION}, even if the rest of the syntax
     * is invalid. This allows the preprocessor to distinguish unsupported assertion syntax from
     * ordinary DDL statements that should remain executable.
     */
    private static final Pattern ASSERTION_PREFIX_PATTERN = Pattern.compile("(?is)^CREATE\\s+ASSERTION\\b.*$");

    /**
     * Splits the input script into statements, removes supported assertions from the executable DDL,
     * and returns both the extracted assertions and any preprocessing errors.
     *
     * @param ddl the raw DDL script that may contain regular DDL statements and assertion definitions
     * @return a result containing sanitized DDL, extracted assertions and error messages
     */
    public PreprocessingResult preprocess(String ddl) {
        if (ddl == null || ddl.isBlank()) {
            return new PreprocessingResult("", List.of(), List.of());
        }

        // Keep the different outputs separate: runnable DDL, extracted assertions, duplicate tracking,
        // and user-facing errors collected during parsing.
        List<ExtractedAssertion> assertions = new ArrayList<>();
        List<String> sanitizedStatements = new ArrayList<>();
        Set<String> duplicateCheck = new LinkedHashSet<>();
        List<String> errors = new ArrayList<>();

        for (String statement : SqlStatementSplitter.splitStatements(ddl)) {
            String trimmedStatement = statement.trim();
            if (trimmedStatement.isBlank()) {
                continue;
            }

            // Leading comments should not affect assertion detection, so strip them before matching.
            String statementWithoutLeadingComments = stripLeadingComments(trimmedStatement);

            // Non-assertion statements remain part of the sanitized script and will be executed later.
            if (!ASSERTION_PREFIX_PATTERN.matcher(statementWithoutLeadingComments).matches()) {
                sanitizedStatements.add(trimmedStatement);
                continue;
            }

            // Assertions are only accepted in one explicit form; everything else is reported as unsupported.
            Matcher matcher = SUPPORTED_ASSERTION_PATTERN.matcher(statementWithoutLeadingComments);
            if (!matcher.matches()) {
                errors.add("Unsupported assertion syntax: " + trimmedStatement);
                continue;
            }

            String name = matcher.group(1).trim();
            String definitionSql = matcher.group(2).trim();

            if (definitionSql.isBlank()) {
                errors.add("Assertion '" + name + "' has an empty CHECK condition.");
                continue;
            }

            String normalizedName = normalizeName(name);
            if (!duplicateCheck.add(normalizedName)) {
                errors.add("Duplicate assertion definition '" + name + "'.");
                continue;
            }

            assertions.add(new ExtractedAssertion(name, definitionSql));
        }

        return new PreprocessingResult(
            toSanitizedDdl(sanitizedStatements),
            assertions,
            errors.stream().distinct().toList()
        );
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    /**
     * Removes comments that appear before the actual SQL statement body.
     *
     * @param statement a single SQL statement that may start with line or block comments
     * @return the statement without leading comments and leading whitespace
     */
    private String stripLeadingComments(String statement) {
        String remaining = statement;

        // Repeatedly peel off comments because several comment blocks may precede the actual SQL keyword.
        while (true) {
            String trimmed = remaining.stripLeading();
            if (trimmed.startsWith("--")) {
                // Remove one line comment and continue in case another comment follows immediately.
                int lineBreakIndex = trimmed.indexOf('\n');
                remaining = lineBreakIndex >= 0 ? trimmed.substring(lineBreakIndex + 1) : "";
                continue;
            }

            if (trimmed.startsWith("/*")) {
                // Remove one block comment and continue scanning for further leading comments.
                int commentEndIndex = trimmed.indexOf("*/");
                remaining = commentEndIndex >= 0 ? trimmed.substring(commentEndIndex + 2) : "";
                continue;
            }

            // Stop once the remaining content starts with actual SQL or becomes empty.
            return trimmed;
        }
    }

    /**
     * Reassembles the executable DDL from the non-assertion statements.
     *
     * @param statements the statements that should remain executable
     * @return a newline-separated DDL script where each statement is semicolon-terminated
     */
    private String toSanitizedDdl(List<String> statements) {
        if (statements.isEmpty()) {
            return "";
        }

        return statements.stream()
            .reduce((left, right) -> left + System.lineSeparator() + right)
            .orElse("");
    }
}
