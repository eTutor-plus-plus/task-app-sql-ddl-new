package at.jku.dke.task_app.sql_ddl.validation;

import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskGroupDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidTaskGroupNumberValidatorTest {

    @Test
    void isValidCorrectOrder() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifySQLDDLTaskGroupDto modifySQLDDLTaskGroupDto = new ModifySQLDDLTaskGroupDto(1, 2);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifySQLDDLTaskGroupDto, null);

        // Assert
        assertTrue(result);
    }

    @Test
    void isValidSameValue() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifySQLDDLTaskGroupDto modifySQLDDLTaskGroupDto = new ModifySQLDDLTaskGroupDto(2, 2);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifySQLDDLTaskGroupDto, null);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidIncorrectOrder() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifySQLDDLTaskGroupDto modifySQLDDLTaskGroupDto = new ModifySQLDDLTaskGroupDto(2, 1);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifySQLDDLTaskGroupDto, null);

        // Assert
        assertFalse(result);
    }
}
