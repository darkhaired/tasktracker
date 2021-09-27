package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DateTasks {
    @JsonProperty("date")
    private String date;
    @JsonProperty("tasks")
    private List<TaskModel> tasks;
}
