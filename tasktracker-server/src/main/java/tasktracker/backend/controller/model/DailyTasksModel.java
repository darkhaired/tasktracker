package tasktracker.backend.controller.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Setter
public class DailyTasksModel {
    @JsonProperty("tasks")
    private Map<String, Map<String, List<TaskModel>>> tasks;
}
