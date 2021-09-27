package tasktracker.backend.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.model.Subscription;
import tasktracker.backend.repository.SubscriptionRepository;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v2")
public class SubscriptionController {
    private final SubscriptionRepository repository;

    @PostMapping("/subscriptions")
    public ResponseEntity<?> createSubscription(@RequestBody final SubscriptionBody body) {

        final Subscription subscription = new Subscription();
        subscription.setAddress(body.getAddress());
        subscription.setEventType(body.getEventType());
        subscription.setSubscriptionType(body.getSubscriptionType());

        repository.save(subscription);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<?> deleteSubscription(@PathVariable("id") final Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> getAllSubscriptions() {
        return ResponseEntity.ok(repository.findAll());
    }

    public static class SubscriptionBody {
        @JsonProperty("address")
        private String address;
        // oneTime
        // endless
        @JsonProperty("subscription_type")
        private String subscriptionType;
        // release
        // task completion
        @JsonProperty("event_type")
        private String eventType;

        public String getAddress() {
            return address;
        }

        public String getSubscriptionType() {
            return subscriptionType;
        }

        public String getEventType() {
            return eventType;
        }

        @Override
        public String toString() {
            return "Subscription{" +
                    "address='" + address + '\'' +
                    ", subscriptionType='" + subscriptionType + '\'' +
                    ", eventType='" + eventType + '\'' +
                    '}';
        }
    }
}
