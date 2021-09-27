package tasktracker.backend.controller.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import tasktracker.backend.model.TaskError;

@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskErrorModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("task_id")
    private Long taskId;
    @JsonProperty("type")
    private String type;

    public TaskErrorModel(final TaskError taskError) {
        this.id = taskError.getId();
        this.reason = taskError.getReason();
        this.taskId = taskError.getTask().getId();
        this.type = taskError.getType().name();
    }

    public TaskError toTaskError() {
        final TaskError taskError = new TaskError();
        taskError.setId(id);
        taskError.setReason(reason);
        taskError.setType(TaskError.Type.valueOf(type.toUpperCase().trim()));

        return taskError;
    }
}
