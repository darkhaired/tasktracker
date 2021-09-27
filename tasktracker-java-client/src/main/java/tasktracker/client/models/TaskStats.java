package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskStats {
    @JsonProperty("count")
    @Accessors(chain = true)
    private Long count;
    @JsonProperty("total_count")
    @Accessors(chain = true)
    private Long totalCount;
    @JsonProperty("unique_count")
    @Accessors(chain = true)
    private Long uniqueCount;
    @JsonProperty("column")
    @Accessors(chain = true)
    private String column;
    @JsonProperty(value = "column_type")
    private ColumnType columnType = ColumnType.STRING;
    @JsonProperty("mean")
    @Accessors(chain = true)
    private Double mean;
    @JsonProperty("std_dev")
    @Accessors(chain = true)
    private Double stdDev;
    @JsonProperty("min")
    @Accessors(chain = true)
    private Double min;
    @JsonProperty("max")
    @Accessors(chain = true)
    private Double max;
    @JsonProperty("quantile_5")
    @Accessors(chain = true)
    private Double quantile5;
    @JsonProperty("quantile_15")
    @Accessors(chain = true)
    private Double quantile15;
    @JsonProperty("quantile_25")
    @Accessors(chain = true)
    private Double quantile25;
    @JsonProperty("quantile_50")
    @Accessors(chain = true)
    private Double quantile50;
    @JsonProperty("quantile_75")
    @Accessors(chain = true)
    private Double quantile75;
    @JsonProperty("quantile_90")
    @Accessors(chain = true)
    private Double quantile90;
    @JsonProperty("quantile_95")
    @Accessors(chain = true)
    private Double quantile95;

    public enum ColumnType {
        STRING,
        NUMERIC,
        OBJECT
    }
}