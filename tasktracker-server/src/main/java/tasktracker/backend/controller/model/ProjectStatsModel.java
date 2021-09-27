package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Setter
@NoArgsConstructor
public class ProjectStatsModel {
    @JsonProperty("dates")
    private List<DailyProjectStatsModel> dates = Lists.newArrayList();

    @Data
    @NoArgsConstructor
    public static class DailyProjectStatsModel {
        @JsonProperty("date")
        private String date;
        @JsonProperty("total_tasks")
        private int totalTasks;
        @JsonProperty("total_completed_tasks")
        private int totalCompletedTasks;
        @JsonProperty("total_failed_tasks")
        private int totalFailedTasks;
        @JsonProperty("total_running_tasks")
        private int totalRunningTasks;
        @JsonProperty("total_distinct_tasks")
        private int totalDistinctTasks;
    }
}
