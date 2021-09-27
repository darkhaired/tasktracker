package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tasktracker.backend.controller.DateTimePatterns;
import tasktracker.backend.model.Warning;

import java.util.Objects;

@Data
@NoArgsConstructor
@ToString(of = {"id", "message"})
public class WarningModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("message")
    private String message;
    @JsonProperty("task_id")
    private Long taskId;
    @JsonProperty("created_time")
    private String createdTime;

    public WarningModel(final Warning warning) {
        id = warning.getId();
        message = warning.getMessage();
        taskId = Objects.nonNull(warning.getTask()) ? warning.getTask().getId() : null;
        createdTime = Objects.nonNull(warning.getCreatedTime()) ? DateTimePatterns.getFormattedDateTimeString(warning.getCreatedTime()) : null;
    }

    public Warning toWarning() {
        Warning warning = new Warning();
        warning.setMessage(message);
        return warning;
    }
}
