package tasktracker.backend.controller.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DistributionChart {
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("time_period")
    private String timePeriod;
    @JsonProperty("data")
    private List<Distribution> data;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Distribution {
        @JsonProperty("datetime")
        private String datetime;
        @JsonProperty("counter")
        private int counter;
    }
}
