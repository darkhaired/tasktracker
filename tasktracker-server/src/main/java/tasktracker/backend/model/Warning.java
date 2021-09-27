package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity(name = "Warning")
@Table(name = "warning",
        indexes = {
                @Index(columnList = "task_id", name = "warning_task_id_index")
        }
)
public class Warning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(name = "created_time", nullable = false)
    private Date createdTime = new Date();

    @Override
    public String toString() {
        return "Warning{" +
                "id=" + id +
                ", task=" + task.getId() +
                ", message='" + message + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
}
