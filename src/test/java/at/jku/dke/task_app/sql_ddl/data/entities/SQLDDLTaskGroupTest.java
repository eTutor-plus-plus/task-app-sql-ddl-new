package at.jku.dke.task_app.sql_ddl.data.entities;

import at.jku.dke.etutor.task_app.dto.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SQLDDLTaskGroupTest {

    @Test
    void testConstructor1() {
        // Arrange
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        SQLDDLTaskGroup SQLDDLTaskGroup = new SQLDDLTaskGroup(expectedMinNumber, expectedMaxNumber);
        int actualMinNumber = SQLDDLTaskGroup.getMinNumber();
        int actualMaxNumber = SQLDDLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testConstructor2() {
        // Arrange
        final TaskStatus status = TaskStatus.APPROVED;
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        SQLDDLTaskGroup SQLDDLTaskGroup = new SQLDDLTaskGroup(status, expectedMinNumber, expectedMaxNumber);
        TaskStatus actualStatus = SQLDDLTaskGroup.getStatus();
        int actualMinNumber = SQLDDLTaskGroup.getMinNumber();
        int actualMaxNumber = SQLDDLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(status, actualStatus);
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testConstructor3() {
        // Arrange
        final long expectedId = 21;
        final TaskStatus status = TaskStatus.APPROVED;
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        SQLDDLTaskGroup SQLDDLTaskGroup = new SQLDDLTaskGroup(expectedId, status, expectedMinNumber, expectedMaxNumber);
        long actualId = SQLDDLTaskGroup.getId();
        TaskStatus actualStatus = SQLDDLTaskGroup.getStatus();
        int actualMinNumber = SQLDDLTaskGroup.getMinNumber();
        int actualMaxNumber = SQLDDLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expectedId, actualId);
        assertEquals(status, actualStatus);
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testGetSetMinNumber() {
        // Arrange
        SQLDDLTaskGroup SQLDDLTaskGroup = new SQLDDLTaskGroup();
        final int expected = 21;

        // Act
        SQLDDLTaskGroup.setMinNumber(expected);
        int actual = SQLDDLTaskGroup.getMinNumber();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetSetMaxNumber() {
        // Arrange
        SQLDDLTaskGroup SQLDDLTaskGroup = new SQLDDLTaskGroup();
        final int expected = 21;

        // Act
        SQLDDLTaskGroup.setMaxNumber(expected);
        int actual = SQLDDLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expected, actual);
    }

}
