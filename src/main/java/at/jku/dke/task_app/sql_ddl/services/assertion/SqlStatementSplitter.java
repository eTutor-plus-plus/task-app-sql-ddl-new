package at.jku.dke.task_app.sql_ddl.services.assertion;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits SQL scripts into semicolon-terminated statements while respecting comments,
 * string literals and quoted identifiers.
 * NOTE: This class was created by AI.
 */
public final class SqlStatementSplitter {
    private SqlStatementSplitter() {
    }

    /**
     * Splits a DDL script into semicolon-terminated statements while respecting SQL string literals,
     * quoted identifiers, and comments.
     *
     * @param sql the raw DDL script
     * @return the statements in their original order, without the delimiting semicolons
     */
    public static List<String> splitStatements(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }

        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        // Use a character-based state machine so semicolons inside literals or comments do not split statements.
        for (int index = 0; index < sql.length(); index++) {
            char currentChar = sql.charAt(index);
            char nextChar = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

            if (inLineComment) {
                // Copy line comments verbatim until a newline ends the comment context.
                current.append(currentChar);
                if (currentChar == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                // Copy block comments verbatim until the closing */ token is reached.
                current.append(currentChar);
                if (currentChar == '*' && nextChar == '/') {
                    current.append(nextChar);
                    index++;
                    inBlockComment = false;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && currentChar == '-' && nextChar == '-') {
                // Enter a line comment only when not currently protected by a quoted section.
                current.append(currentChar).append(nextChar);
                index++;
                inLineComment = true;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && currentChar == '/' && nextChar == '*') {
                // Enter a block comment only when not currently protected by a quoted section.
                current.append(currentChar).append(nextChar);
                index++;
                inBlockComment = true;
                continue;
            }

            if (currentChar == '\'' && !inDoubleQuote) {
                current.append(currentChar);
                if (inSingleQuote && nextChar == '\'') {
                    // SQL escapes a single quote by doubling it, so stay inside the string literal.
                    current.append(nextChar);
                    index++;
                } else {
                    // Toggle the string-literal state when an unescaped quote is encountered.
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }

            if (currentChar == '"' && !inSingleQuote) {
                current.append(currentChar);
                if (inDoubleQuote && nextChar == '"') {
                    // Double quotes can be escaped the same way inside quoted identifiers.
                    current.append(nextChar);
                    index++;
                } else {
                    // Toggle the quoted-identifier state when an unescaped double quote is encountered.
                    inDoubleQuote = !inDoubleQuote;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && currentChar == ';') {
                // Only a semicolon outside quoted sections terminates the current statement.
                current.append(currentChar);
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        // Preserve the final fragment even when the script does not end with a semicolon.
        if (current.length() > 0) {
            statements.add(current.toString());
        }

        return statements;
    }
}
