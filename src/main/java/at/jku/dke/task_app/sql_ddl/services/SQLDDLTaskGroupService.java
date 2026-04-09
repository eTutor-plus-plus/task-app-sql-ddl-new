package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTaskGroup;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskGroupRepository;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskGroupDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link SQLDDLTaskGroup}s.
 */
@Service
public class SQLDDLTaskGroupService extends BaseTaskGroupService<SQLDDLTaskGroup, ModifySQLDDLTaskGroupDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link SQLDDLTaskGroupService}.
     *
     * @param repository    The task group repository.
     * @param messageSource The message source.
     */
    public SQLDDLTaskGroupService(SQLDDLTaskGroupRepository repository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    @Override
    protected SQLDDLTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("sql-ddl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        return new SQLDDLTaskGroup(modifyTaskGroupDto.additionalData().minNumber(), modifyTaskGroupDto.additionalData().maxNumber());
    }

    @Override
    protected void updateTaskGroup(SQLDDLTaskGroup taskGroup, ModifyTaskGroupDto<ModifySQLDDLTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("sql-ddl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        taskGroup.setMinNumber(modifyTaskGroupDto.additionalData().minNumber());
        taskGroup.setMaxNumber(modifyTaskGroupDto.additionalData().maxNumber());
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(SQLDDLTaskGroup taskGroup, boolean create) {
        return new TaskGroupModificationResponseDto(
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.ENGLISH));
    }
}
