package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskStatsResponse {
    @JsonProperty("count")
    private Long count;
    @JsonProperty("total_count")
    private Long totalCount;
    @JsonProperty("unique_count")
    private Long uniqueCount;
    @JsonProperty("column")
    private String column;
    @JsonProperty("column_type")
    private String columnType;
    @JsonProperty("mean")
    private Double mean;
    @JsonProperty("std_dev")
    private Double stdDev;
    @JsonProperty("min")
    private Double min;
    @JsonProperty("max")
    private Double max;
    @JsonProperty("quantile_5")
    private Double quantile5;
    @JsonProperty("quantile_15")
    private Double quantile15;
    @JsonProperty("quantile_25")
    private Double quantile25;
    @JsonProperty("quantile_50")
    private Double quantile50;
    @JsonProperty("quantile_75")
    private Double quantile75;
    @JsonProperty("quantile_90")
    private Double quantile90;
    @JsonProperty("quantile_95")
    private Double quantile95;
}
