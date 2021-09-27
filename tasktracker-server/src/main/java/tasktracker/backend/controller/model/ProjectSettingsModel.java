package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import tasktracker.backend.model.ProjectSettings;

@ToString
@Getter
public class ProjectSettingsModel {
    @JsonProperty("auto_status_update")
    private Boolean autoStatusUpdate;
    @JsonProperty("auto_status_update_frequency")
    private Long autoStatusUpdateFrequency;
    @JsonProperty("incident_generation_allowed")
    private Boolean incidentGenerationAllowed;
    @JsonProperty("task_synchronization_allowed")
    private Boolean taskSynchronizationAllowed;
    @JsonProperty("task_data_quality_allowed")
    private Boolean taskDataQualityAllowed;

    public ProjectSettingsModel() {
        // NO OP
    }

    public ProjectSettingsModel(final ProjectSettings settings) {
        this.autoStatusUpdate = settings.getAutoStatusUpdate();
        this.autoStatusUpdateFrequency = settings.getAutoStatusUpdateFrequency();
        this.incidentGenerationAllowed = settings.getIncidentGenerationAllowed();
        this.taskDataQualityAllowed = settings.getTaskDataQualityAllowed();
        this.taskSynchronizationAllowed = settings.getTaskSynchronizationAllowed();
    }

    public ProjectSettings to() {
        final ProjectSettings settings = new ProjectSettings();
        settings.setAutoStatusUpdate(autoStatusUpdate);
        settings.setAutoStatusUpdateFrequency(autoStatusUpdateFrequency);
        settings.setIncidentGenerationAllowed(incidentGenerationAllowed);
        settings.setTaskDataQualityAllowed(taskDataQualityAllowed);
        settings.setTaskSynchronizationAllowed(taskSynchronizationAllowed);

        return settings;
    }
}
