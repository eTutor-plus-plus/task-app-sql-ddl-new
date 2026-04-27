package at.jku.dke.task_app.sql_ddl.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLCheckConstraintDto;
import at.jku.dke.task_app.sql_ddl.dto.SQLDDLTaskDto;
import at.jku.dke.task_app.sql_ddl.dto.ModifySQLDDLTaskDto;
import at.jku.dke.task_app.sql_ddl.services.SQLDDLTaskService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing {@link SQLDDLTask}s.
 */
@RestController
public class TaskController extends BaseTaskController<SQLDDLTask, SQLDDLTaskDto, ModifySQLDDLTaskDto> {

    /**
     * Creates a new instance of class {@link TaskController}.
     *
     * @param taskService The task service.
     */
    public TaskController(SQLDDLTaskService taskService) {
        super(taskService);
    }

    @Override
    protected SQLDDLTaskDto mapToDto(SQLDDLTask task) {
        return new SQLDDLTaskDto(
            task.getSolution(),
            task.getTablePoints(),
            task.getPrimaryKeyPoints(),
            task.getForeignKeyPoints(),
            task.getConstraintPoints(),
            task.getWhitelist(),
            task.getCheckConstraints() == null
                ? List.of()
                : task.getCheckConstraints().stream()
                .map(constraint -> new SQLDDLCheckConstraintDto(
                    constraint.getDefinition(),
                    constraint.getSuccessfulInsertStatements(),
                    constraint.getUnsuccessfulInsertStatements()
                ))
                .toList()
        );
    }

}
