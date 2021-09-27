package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tasktracker.backend.model.TaskType;

@NoArgsConstructor
@Getter
@Setter
public class TaskTypeModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("project_id")
    private Long projectId;
    @JsonProperty("name")
    private String name;

    public TaskTypeModel(final TaskType type) {
        this.id = type.getId();
        this.projectId = type.getProject().getId();
        this.name = type.getName();
    }

    @JsonIgnore
    public TaskType toTaskType() {
        final TaskType type = new TaskType();
        type.setName(name);
        return type;
    }
}
