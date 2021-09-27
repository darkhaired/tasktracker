package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.ProjectSettings;

import java.util.Optional;

@Repository
public interface ProjectSettingsRepository extends CrudRepository<ProjectSettings, Long> {

    Optional<ProjectSettings> findByProjectId(long projectId);

}
