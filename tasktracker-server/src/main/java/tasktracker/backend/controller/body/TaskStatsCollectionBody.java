package tasktracker.backend.controller.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class TaskStatsCollectionBody {
    @JsonProperty("statistics")
    private List<TaskStatsBody> stats;
}
