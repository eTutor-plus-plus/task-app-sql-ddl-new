package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SQLDDLTaskServiceTest {

    @Test
    void createTask() {
        // Arrange
        ModifyTaskDto<ModifySQLDDLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql-ddl", TaskStatus.APPROVED, new ModifySQLDDLTaskDto(33));
        SQLDDLTaskService service = new SQLDDLTaskService(null, null, null);

        // Act
        SQLDDLTask task = service.createTask(3, dto);

        // Assert
        assertEquals(dto.additionalData().solution(), task.getSolution());
    }

    @Test
    void createTaskInvalidType() {
        // Arrange
        ModifyTaskDto<ModifySQLDDLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql", TaskStatus.APPROVED, new ModifySQLDDLTaskDto(33));
        SQLDDLTaskService service = new SQLDDLTaskService(null, null, null);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.createTask(3, dto));
    }

    @Test
    void updateTask() {
        // Arrange
        ModifyTaskDto<ModifySQLDDLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql-ddl", TaskStatus.APPROVED, new ModifySQLDDLTaskDto(33));
        SQLDDLTaskService service = new SQLDDLTaskService(null, null, null);
        SQLDDLTask task = new SQLDDLTask(3);

        // Act
        service.updateTask(task, dto);

        // Assert
        assertEquals(dto.additionalData().solution(), task.getSolution());
    }

    @Test
    void updateTaskInvalidType() {
        // Arrange
        ModifyTaskDto<ModifySQLDDLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql", TaskStatus.APPROVED, new ModifySQLDDLTaskDto(33));
        SQLDDLTaskService service = new SQLDDLTaskService(null, null, null);
        SQLDDLTask task = new SQLDDLTask(3);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.updateTask(task, dto));
    }

    @Test
    void mapToReturnData() {
        // Arrange
        MessageSource ms = mock(MessageSource.class);
        SQLDDLTaskService service = new SQLDDLTaskService(null, null, ms);
        SQLDDLTask task = new SQLDDLTask(3);
        task.setSolution(33);

        // Act
        TaskModificationResponseDto result = service.mapToReturnData(task, true);

        // Assert
        assertNotNull(result);
        verify(ms).getMessage("defaultTaskDescription", null, Locale.GERMAN);
        verify(ms).getMessage("defaultTaskDescription", null, Locale.ENGLISH);
    }

}
