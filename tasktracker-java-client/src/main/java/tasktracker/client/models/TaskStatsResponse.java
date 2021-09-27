package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class TaskStatsResponse {
    @JsonProperty("statistics")
    private List<TaskStats> statistics;
}
