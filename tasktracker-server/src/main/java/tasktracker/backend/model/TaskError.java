package tasktracker.backend.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Data
@NoArgsConstructor
@ToString(of = {"id", "reason", "type", "timestamp"})
@Entity(name = "TaskError")
@Table(name = "task_error", indexes = {@Index(columnList = "task_state_id", name = "errors_task_state_id")})
public class TaskError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @ManyToOne
    @JoinColumn(name = "task_state_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;
    @Column(name = "reason")
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;
    @Column(name = "timestamp")
    private Long timestamp = System.currentTimeMillis();

    public enum Type {
        DATA_LOAD_ERROR,
        NO_DATA_ERROR,
        MODEL_NOT_FOUND_ERROR,
        RUNTIME_ERROR;
    }
}
