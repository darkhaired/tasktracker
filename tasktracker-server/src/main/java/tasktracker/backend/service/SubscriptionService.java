package tasktracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tasktracker.backend.repository.SubscriptionRepository;

@Service
public class SubscriptionService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionRepository repository;

    @Autowired
    public SubscriptionService(
            final SubscriptionRepository repository
    ) {
        this.repository = repository;
    }


}
