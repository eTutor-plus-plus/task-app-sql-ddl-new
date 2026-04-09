package at.jku.dke.task_app.sql_ddl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskInGroupService;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTaskGroup;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskGroupRepository;
import at.jku.dke.task_app.sql_ddl.data.repositories.SQLDDLTaskRepository;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link SQLDDLTask}s.
 */
@Service
public class SQLDDLTaskService extends BaseTaskInGroupService<SQLDDLTask, SQLDDLTaskGroup, ModifySQLDDLTaskDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link SQLDDLTaskService}.
     *
     * @param repository          The task repository.
     * @param taskGroupRepository The task group repository.
     * @param messageSource       The message source.
     */
    public SQLDDLTaskService(SQLDDLTaskRepository repository, SQLDDLTaskGroupRepository taskGroupRepository, MessageSource messageSource) {
        super(repository, taskGroupRepository);
        this.messageSource = messageSource;
    }

    @Override
    protected SQLDDLTask createTask(long id, ModifyTaskDto<ModifySQLDDLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("sql-ddl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        return new SQLDDLTask(modifyTaskDto.additionalData().solution());
    }

    @Override
    protected void updateTask(SQLDDLTask task, ModifyTaskDto<ModifySQLDDLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("sql-ddl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        task.setSolution(modifyTaskDto.additionalData().solution());
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(SQLDDLTask task, boolean create) {
        return new TaskModificationResponseDto(
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.ENGLISH)
        );
    }
}
