package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity(name = "Incident")
@Table(name = "incident", indexes = {
        @Index(columnList = "task_id", name = "incident_index_on_task_id")
})
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "date")
    private Date date;
    @Column(name = "url", columnDefinition = "TEXT", length = 1000)
    private String url;
    @Column(name = "title", columnDefinition = "TEXT", length = 1000)
    private String title;
    @Column(name = "description", columnDefinition = "TEXT", length = 10000)
    private String description;
    @OneToOne
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private Task task;

    @Override
    public String toString() {
        return "Incident{" +
                "id=" + id +
                ", date=" + date +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", task=" + task.getId() +
                '}';
    }
}
