package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@NoArgsConstructor
public class TasksReportModel {
    @JsonProperty("name")
    private String name;
    @JsonProperty("dates")
    private List<DateTasks> dates;
}
