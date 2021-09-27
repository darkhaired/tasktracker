package tasktracker.backend.model;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(name = "data_quality_rule")
public class DataQualityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(name = "table_name")
    private String tableName;
    @Column(name = "task_name")
    private String taskName;
    @Column(name = "caption")
    private String caption;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataQualityRule", fetch = FetchType.EAGER)
    private List<DataQualityCondition> conditions = Lists.newArrayList();

    @Override
    public String toString() {
        return "DataQualityRule{" +
                "id=" + id +
                ", project_id=" + project.getId() +
                ", tableName='" + tableName + '\'' +
                ", taskName='" + taskName + '\'' +
                ", caption='" + caption + '\'' +
                ", conditions=" + conditions +
                '}';
    }
}
