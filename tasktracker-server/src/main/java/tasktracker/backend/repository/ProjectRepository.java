package tasktracker.backend.repository;

import com.google.common.collect.Lists;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends CrudRepository<Project, Long> {

    default List<Project> findProjects() {
        return Lists.newArrayList(findAll());
    }

    Optional<Project> findByNameIgnoreCase(String name);

}
