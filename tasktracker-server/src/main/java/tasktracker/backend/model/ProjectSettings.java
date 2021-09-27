package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity(name="ProjectSettings")
@Table(name="project_settings", indexes = {@Index(columnList = "project_id", name="project_settings_index_on_project_id")})
public class ProjectSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    @OneToOne
    @JoinColumn(name="project_id", nullable=false)
    private Project project;
    @Column(name="auto_status_update")
    private Boolean autoStatusUpdate = false;
    @Column(name="auto_status_update_frequency")
    private Long autoStatusUpdateFrequency = TimeUnit.HOURS.toMinutes(1);
    @Column(name="incident_generation_allowed")
    private Boolean incidentGenerationAllowed = false;
    @Column(name = "task_synchronization_allowed", nullable = false, columnDefinition = "boolean default false")
    private Boolean taskSynchronizationAllowed = false;
    @Column(name = "task_data_quality_allowed", nullable = false, columnDefinition = "boolean default false")
    private Boolean taskDataQualityAllowed = false;
}
