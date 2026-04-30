package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.task_app.sql_ddl.dto.SQLDDLAssertionDto;
import at.jku.dke.task_app.sql_ddl.evaluation.SchemaMetadataExtractor;
import at.jku.dke.task_app.sql_ddl.services.assertion.AssertionConditionEvaluator;
import at.jku.dke.task_app.sql_ddl.services.assertion.AssertionScriptPreprocessor;
import at.jku.dke.task_app.sql_ddl.services.feedback.WhitelistWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SQLDDLTaskServiceTest {
    private SQLDDLTaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new SQLDDLTaskService(
            null,
            null,
            new SchemaMetadataExtractor(),
            new WhitelistWordService(),
            new AssertionScriptPreprocessor(),
            new AssertionConditionEvaluator()
        );
    }

    @Test
    void prepareTaskArtifactsRejectsUnsuccessfulAssertionStatementsThatOnlyFailCollectively() {
        String ddl = """
            CREATE TABLE projekt (
                projekt_id INTEGER,
                code VARCHAR(20) NOT NULL,
                name VARCHAR(100) NOT NULL,
                startdatum DATE NOT NULL,
                enddatum DATE,
                budget DECIMAL(12,2) NOT NULL,
                PRIMARY KEY (projekt_id),
                UNIQUE (code),
                UNIQUE (name, startdatum)
            );

            CREATE ASSERTION chk_projekt_datum
            CHECK (
                NOT EXISTS (
                    SELECT 1
                    FROM projekt
                    WHERE enddatum IS NOT NULL
                      AND enddatum < startdatum
                )
            );
            """;

        SQLDDLAssertionDto assertionDto = new SQLDDLAssertionDto(
            "chk_projekt_datum",
            """
                INSERT INTO projekt VALUES
                (1, 'P001', 'Validierungsprojekt', DATE '2024-01-01', DATE '2024-12-31', 10000);
                """,
            """
                INSERT INTO projekt VALUES
                (5, 'P005', 'Fehlerprojekt2', DATE '2026-01-01', DATE '2024-12-31', 30000);

                INSERT INTO projekt VALUES
                (4, 'P004', 'Fehlerprojekt1', DATE '2022-06-01', DATE '2024-05-01', 10000);
                """
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> ReflectionTestUtils.invokeMethod(taskService, "prepareTaskArtifacts", ddl, List.of(), List.of(assertionDto))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
            "Unsuccessful statements did not violate assertion 'chk_projekt_datum'.",
            exception.getReason()
        );
    }

    @Test
    void prepareTaskArtifactsAcceptsSuccessfulAssertionStatementsIndividually() {
        String ddl = """
            CREATE TABLE einsatz (
                mitarbeiter_id INTEGER NOT NULL,
                stunden INTEGER NOT NULL
            );

            CREATE ASSERTION chk_arbeitszeit
            CHECK (
                NOT EXISTS (
                    SELECT 1
                    FROM einsatz
                    GROUP BY mitarbeiter_id
                    HAVING SUM(stunden) > 40
                )
            );
            """;

        SQLDDLAssertionDto assertionDto = new SQLDDLAssertionDto(
            "chk_arbeitszeit",
            """
                INSERT INTO einsatz VALUES (1, 30);
                INSERT INTO einsatz VALUES (1, 20);
                """,
            """
                INSERT INTO einsatz VALUES (1, 50);
                """
        );

        Object artifacts = assertDoesNotThrow(
            () -> ReflectionTestUtils.invokeMethod(taskService, "prepareTaskArtifacts", ddl, List.of(), List.of(assertionDto))
        );

        assertNotNull(artifacts);
    }
}
