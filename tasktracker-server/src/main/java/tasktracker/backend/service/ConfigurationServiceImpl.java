package tasktracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.model.Configuration;
import tasktracker.backend.repository.ConfigurationRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ConfigurationServiceImpl implements ConfigurationService {
    private final ConfigurationRepository repository;

    @Transactional(readOnly = true)
    @Override
    public List<Configuration> all() {
        return repository.findAll();
    }

    @Transactional
    @Override
    public Configuration save(final Configuration configuration) {
        return repository.save(configuration);
    }

    @Transactional
    @Override
    public Configuration update(final Configuration configuration) {
        return repository.save(configuration);
    }

    @Transactional
    @Override
    public void delete(final Configuration configuration) {
        repository.delete(configuration);
    }

    @Override
    public Optional<Configuration> findByKey(final String key) {
        return repository.findByKey(key);
    }
}
