package at.jku.dke.task_app.sql_ddl.validation;

import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskGroupDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom validator for numbers in {@link ModifySQLDDLTaskGroupDto}.
 */
public class ValidTaskGroupNumberValidator implements ConstraintValidator<ValidTaskGroupNumber, ModifySQLDDLTaskGroupDto> {
    /**
     * Creates a new instance of class Valid task group number validator.
     */
    public ValidTaskGroupNumberValidator() {
    }

    @Override
    public boolean isValid(ModifySQLDDLTaskGroupDto value, ConstraintValidatorContext context) {
        return value.minNumber() < value.maxNumber();
    }
}
