package at.jku.dke.task_app.sql_ddl.data.repositories;

import at.jku.dke.etutor.task_app.data.repositories.TaskRepository;
import at.jku.dke.task_app.sql_ddl.data.entities.SQLDDLTask;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

/**
 * Repository for entity {@link SQLDDLTask}.
 */
public interface SQLDDLTaskRepository extends TaskRepository<SQLDDLTask> {
    @Override
    @EntityGraph(attributePaths = {"checkConstraints", "assertions"})
    Optional<SQLDDLTask> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"checkConstraints", "assertions"})
    List<SQLDDLTask> findAll();
}
