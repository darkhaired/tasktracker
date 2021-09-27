package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;

import java.util.List;

@Repository
public interface DataQualityRuleRepository extends CrudRepository<DataQualityRule, Long> {
    List<DataQualityRule> findByProject(Project project);
}
