package tasktracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.Subscription;

import java.util.Collection;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Collection<Subscription> findByEventType(String event);

}
