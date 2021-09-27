package tasktracker.backend.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity(name = "Subscription")
@Table(name = "subscription")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "address", length = 500, nullable = false)
    private String address;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "subscription_type", nullable = false)
    private String subscriptionType;
}
