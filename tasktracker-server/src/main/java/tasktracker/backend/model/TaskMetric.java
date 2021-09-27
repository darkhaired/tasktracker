package tasktracker.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity(name = "TaskMetric")
@Table(name = "task_metric", indexes = {@Index(columnList = "task_state_id", name = "metrics_task_state_id")})
public class TaskMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @ManyToOne
    @JoinColumn(name = "task_state_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;
    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private Double value;
    @Column(name = "value_2")
    private String value2;
    @Column(name = "timestamp")
    private Long timestamp = System.currentTimeMillis();
    @Column(name = "label", nullable = true)
    private String label;
}
