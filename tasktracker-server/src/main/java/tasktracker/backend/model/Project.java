package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity(name = "Project")
@Table(name = "project")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<Task> taskStates;
    @Column(name = "created_at")
    private Date createdAt;
    @OneToOne(mappedBy = "project", cascade = {CascadeType.ALL})
    private ProjectSettings settings;

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
