package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.DataQualityRule;

import java.util.List;

@Repository
public interface DataQualityConditionRepository extends CrudRepository<DataQualityCondition, Long> {
    List<DataQualityCondition> findByDataQualityRule(DataQualityRule rule);
}
