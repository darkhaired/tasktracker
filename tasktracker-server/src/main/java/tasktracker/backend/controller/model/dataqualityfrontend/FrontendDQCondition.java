package tasktracker.backend.controller.model.dataqualityfrontend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import tasktracker.backend.dq.DataQualityFunction;
import tasktracker.backend.model.DataQualityCondition;

import java.util.List;
import java.util.Map;

import static tasktracker.backend.dq.DataQualityConditionModelValidator.extractArguments;
import static tasktracker.backend.dq.DataQualityConditionModelValidator.extractFunctionName;

@Getter
@Setter
public class FrontendDQCondition {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("column_name")
    private String columnName;
    @JsonProperty("metric")
    private String metric;
    @JsonProperty("functionName")
    private String function;
    @JsonProperty("args")
    private Map<String, String> args;

    public FrontendDQCondition(DataQualityCondition condition, List<DataQualityFunction> functions) {
        this.id = condition.getId();
        this.columnName = condition.getColumnName();
        this.metric = condition.getMetric().name();
        this.function = extractFunctionName(condition.getExpression());
        this.args = getExpressionsArgs(condition.getExpression(), functions);
    }

    private Map<String, String> getExpressionsArgs(final String expression, final List<DataQualityFunction> functions) {
        Map<String, String> args = Maps.newLinkedHashMap();
        List<String> extractedArgs = extractArguments(expression);
        String functionName = extractFunctionName(expression);
        DataQualityFunction function = functions.stream()
                .filter(dataQualityFunction -> dataQualityFunction.getName().equals(functionName))
                .findFirst().orElse(null);
        List<DataQualityFunction.DataQualityFunctionArgument> dataQualityFunctionArgs = function.getArguments();
        for (int i = 0; i < dataQualityFunctionArgs.size(); i++) {
            args.put(dataQualityFunctionArgs.get(i).getName(), extractedArgs.get(i));
        }

        return args;
    }
}
