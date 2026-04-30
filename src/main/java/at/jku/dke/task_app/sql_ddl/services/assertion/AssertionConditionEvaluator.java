package at.jku.dke.task_app.sql_ddl.services.assertion;

import org.h2.tools.RunScript;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;


@Service
public class AssertionConditionEvaluator {

    public void executeStatements(Connection connection, String statements) throws SQLException {
        if (statements == null || statements.isBlank()) {
            return;
        }

        RunScript.execute(connection, new StringReader(statements));
    }

    /**
     * After inserting all successful/unsuccessful statements the CHECK part of the assertion
     * is evaluated. When the Assertion is satisfied this function returns TRUE.
     *
     * @param connection H2 Database connection
     * @param definitionSql Assertion CHECK
     * @return boolean
     * @throws SQLException
     */
    public boolean isSatisfied(Connection connection, String definitionSql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT CASE WHEN ((" + definitionSql + ") IS NOT FALSE) THEN 1 ELSE 0 END"
             )) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    /**
     * Validates every semicolon terminated statement independently against the same base schema.
     */
    public boolean matchesExpectedOutcomeForEachStatement(
        Connection connection,
        String statements,
        String definitionSql,
        boolean expectedSatisfied
    ) throws SQLException {
        for (String statement : SqlStatementSplitter.splitStatements(statements)) {
            if (statement.trim().isBlank()) {
                continue;
            }

            Savepoint savepoint = connection.setSavepoint();
            try {
                executeStatements(connection, statement);
                if (isSatisfied(connection, definitionSql) != expectedSatisfied) {
                    return false;
                }
            } finally {
                connection.rollback(savepoint);
            }
        }

        return true;
    }
}
