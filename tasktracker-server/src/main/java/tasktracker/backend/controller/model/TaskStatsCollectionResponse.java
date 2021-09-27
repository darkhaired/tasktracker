package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TaskStatsCollectionResponse {
    @JsonProperty("statistics")
    private List<TaskStatsResponse> stats = Lists.newArrayList();
}
