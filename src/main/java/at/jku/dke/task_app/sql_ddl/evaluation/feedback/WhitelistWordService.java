package at.jku.dke.task_app.sql_ddl.evaluation.feedback;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes words from SQL DDL statements and whitelist definitions.
 */
@Service
public class WhitelistWordService {
    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^a-zA-Z0-9_]+");
    private static final Pattern NUMERIC_WORD = Pattern.compile("[0-9]+");

    public String generateWhitelist(String input) {
        return String.join(";", extractWords(input));
    }

    public List<String> findWhitelistViolations(String whitelist, String submission) {
        Set<String> allowedWords = Set.copyOf(extractWords(whitelist));
        return extractWords(submission).stream()
            .filter(word -> !allowedWords.contains(word))
            .toList();
    }

    List<String> extractWords(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        return WORD_SEPARATOR.splitAsStream(input)
            .filter(word -> !word.isBlank())
            .filter(word -> !NUMERIC_WORD.matcher(word).matches())
            .map(String::toLowerCase)
            .distinct()
            .toList();
    }
}
