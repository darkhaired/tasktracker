package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import tasktracker.backend.model.DataQualityCondition;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor
@Getter
@Setter
public class DataQualityConditionModel {
    @JsonProperty("id")
    private Long id;
    @NotBlank(message = "Field 'column_name' should not be blank")
    @JsonProperty("column_name")
    private String columnName;
    @NotBlank(message = "Field 'metric' should not be blank")
    @JsonProperty("metric")
    private String metric;
    @NotBlank(message = "Field 'expression' should not be blank")
    @JsonProperty("expression")
    private String expression;

    public DataQualityConditionModel(final DataQualityCondition condition) {
        this.id = condition.getId();
        this.columnName = condition.getColumnName();
        this.metric = condition.getMetric().name();
        this.expression = condition.getExpression();
    }

    public DataQualityCondition to() {
        DataQualityCondition condition = new DataQualityCondition();
        condition.setId(id);
        condition.setColumnName(StringUtils.trim(columnName));
        condition.setMetric(DataQualityCondition.Metric.valueOf(metric));
        condition.setExpression(StringUtils.replaceAll(expression, "\\s+", ""));
        return condition;
    }
}
