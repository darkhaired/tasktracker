package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TasksReportResponse {
    @JsonProperty("tasks")
    private List<TasksReportModel> tasks;
}
