package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "project", "name"})
@Entity(name = "TaskType")
@Table(name = "task_type")
public class TaskType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @JoinColumn(name = "project_id")
    @ManyToOne
    private Project project;
    @Column(name = "name", unique = true)
    private String name;
}
