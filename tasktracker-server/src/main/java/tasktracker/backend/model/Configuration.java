package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity(name = "Configuration")
@Table(name = "configuration")
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "key", columnDefinition = "TEXT", length = 1000, nullable = false)
    private String key;
    @Column(name = "value", columnDefinition = "TEXT", length = 1000, nullable = false)
    private String value;
}
