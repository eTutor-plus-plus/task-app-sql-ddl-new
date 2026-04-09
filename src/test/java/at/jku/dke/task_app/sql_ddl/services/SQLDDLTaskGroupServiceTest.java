package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTaskGroup;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskGroupDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SQLDDLTaskGroupServiceTest {

    @Test
    void createTaskGroup() {
        // Arrange
        ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> dto = new ModifyTaskGroupDto<>("sql-ddl", TaskStatus.APPROVED, new ModifySQLDDLTaskGroupDto(1, 2));
        SQLDDLTaskGroupService service = new SQLDDLTaskGroupService(null, null);

        // Act
        var taskGroup = service.createTaskGroup(3, dto);

        // Assert
        assertEquals(dto.additionalData().minNumber(), taskGroup.getMinNumber());
        assertEquals(dto.additionalData().maxNumber(), taskGroup.getMaxNumber());
    }

    @Test
    void createTaskGroupInvalidType() {
        // Arrange
        ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> dto = new ModifyTaskGroupDto<>("sql", TaskStatus.APPROVED, new ModifySQLDDLTaskGroupDto(1, 2));
        SQLDDLTaskGroupService service = new SQLDDLTaskGroupService(null, null);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.createTaskGroup(3, dto));
    }

    @Test
    void updateTaskGroup() {
        // Arrange
        ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> dto = new ModifyTaskGroupDto<>("sql-ddl", TaskStatus.APPROVED, new ModifySQLDDLTaskGroupDto(1, 2));
        SQLDDLTaskGroupService service = new SQLDDLTaskGroupService(null, null);
        var taskGroup = new SQLDDLTaskGroup(3, 4);

        // Act
        service.updateTaskGroup(taskGroup, dto);

        // Assert
        assertEquals(dto.additionalData().minNumber(), taskGroup.getMinNumber());
        assertEquals(dto.additionalData().maxNumber(), taskGroup.getMaxNumber());
    }

    @Test
    void updateTaskGroupInvalidType() {
        // Arrange
        ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> dto = new ModifyTaskGroupDto<>("sql", TaskStatus.APPROVED, new ModifySQLDDLTaskGroupDto(1, 2));
        SQLDDLTaskGroupService service = new SQLDDLTaskGroupService(null, null);
        var taskGroup = new SQLDDLTaskGroup(3, 4);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.updateTaskGroup(taskGroup, dto));
    }

    @Test
    void mapToReturnData() {
        // Arrange
        MessageSource ms = mock(MessageSource.class);
        SQLDDLTaskGroupService service = new SQLDDLTaskGroupService(null, ms);
        var taskGroup = new SQLDDLTaskGroup(3, 4);

        // Act
        var result = service.mapToReturnData(taskGroup, true);

        // Assert
        assertNotNull(result);
        verify(ms).getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.GERMAN);
        verify(ms).getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.ENGLISH);
    }

}
