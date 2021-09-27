package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tasktracker.backend.model.Project;
import tasktracker.backend.controller.DateTimePatterns;

@NoArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("created_at")
    private String createdAt;

    public ProjectModel(final Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.createdAt = DateTimePatterns.getFormattedDateTimeString(project.getCreatedAt());
    }

    public Project toProject() {
        final Project project = new Project();
        project.setId(getId());
        project.setName(getName());
        project.setDescription(getDescription());
        project.setCreatedAt(DateTimePatterns.getDateTimeFromFormattedString(getCreatedAt()));
        return project;
    }
}
