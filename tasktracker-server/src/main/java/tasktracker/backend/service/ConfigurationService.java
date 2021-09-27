package tasktracker.backend.service;

import tasktracker.backend.model.Configuration;

import java.util.List;
import java.util.Optional;

public interface ConfigurationService {

    List<Configuration> all();

    Configuration save(Configuration configuration);

    Configuration update(Configuration configuration);

    void delete(Configuration configuration);

    Optional<Configuration> findByKey(String key);

}
