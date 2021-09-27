package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tasktracker.backend.dq.DataQualityFunction;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.TaskStats;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class DataQualityOptions {
    @JsonProperty("functions")
    private List<DataQualityFunctionModel> functionsModel;
    @JsonProperty("tables")
    private List<TableModel> tables;
    @JsonProperty("metrics")
    private List<ColumnTypeToMetricsModel> columnTypeToMetricsModels;

    public DataQualityOptions(List<DataQualityFunctionModel> functionsModel, List<TableModel> tables) {
        this.functionsModel = functionsModel;
        this.tables = tables;
        this.columnTypeToMetricsModels = columnTypeToMetricsModelsCorrespondence();
    }

    public static List<ColumnTypeToMetricsModel> columnTypeToMetricsModelsCorrespondence() {
        List<ColumnTypeToMetricsModel> columnTypeToMetrics = Lists.newArrayList();
        columnTypeToMetrics.add(new ColumnTypeToMetricsModel(TaskStats.ColumnType.STRING,
                Arrays.asList(DataQualityCondition.Metric.count,
                        DataQualityCondition.Metric.total_count,
                        DataQualityCondition.Metric.unique_count
                )));
        columnTypeToMetrics.add(new ColumnTypeToMetricsModel(TaskStats.ColumnType.OBJECT,
                Arrays.asList(DataQualityCondition.Metric.count,
                        DataQualityCondition.Metric.total_count
                )));
        columnTypeToMetrics.add(new ColumnTypeToMetricsModel(TaskStats.ColumnType.NUMERIC,
                Arrays.asList(DataQualityCondition.Metric.count,
                        DataQualityCondition.Metric.total_count,
                        DataQualityCondition.Metric.min,
                        DataQualityCondition.Metric.max,
                        DataQualityCondition.Metric.std_dev,
                        DataQualityCondition.Metric.quantile_5,
                        DataQualityCondition.Metric.quantile_15,
                        DataQualityCondition.Metric.quantile_25,
                        DataQualityCondition.Metric.quantile_50,
                        DataQualityCondition.Metric.quantile_75,
                        DataQualityCondition.Metric.quantile_90,
                        DataQualityCondition.Metric.quantile_95
                )));
        return columnTypeToMetrics;
    }

    @NoArgsConstructor
    @Setter
    public static class DataQualityFunctionModel {
        @JsonProperty("name")
        private String name;
        @JsonProperty("description")
        private String description;
        @JsonProperty("args_num")
        private Integer argsNum;
        @JsonProperty("args_description")
        private List<DataQualityFunctionArgumentModel> arguments;

        public DataQualityFunctionModel(final DataQualityFunction dataQualityFunction) {
            name = dataQualityFunction.getName();
            description = dataQualityFunction.getDescription();
            argsNum = dataQualityFunction.getArgsNum();
            arguments = dataQualityFunction.getArguments()
                    .stream()
                    .map(DataQualityFunctionArgumentModel::new)
                    .collect(Collectors.toList());
        }
    }

    @NoArgsConstructor
    @Setter
    public static class DataQualityFunctionArgumentModel {
        @JsonProperty("name")
        private String name;
        @JsonProperty("type")
        private String argumentType;
        @JsonProperty("fixed")
        private List<String> fixedValues;
        @JsonProperty("placeholder")
        private String placeholder;
        @JsonProperty("description")
        private String description;

        public DataQualityFunctionArgumentModel(final DataQualityFunction.DataQualityFunctionArgument functionArgument) {
            name = functionArgument.getName();
            argumentType = functionArgument.getArgumentType().toString().toLowerCase();
            fixedValues = functionArgument.getFixedValues();
            placeholder = functionArgument.getPlaceholder();
            description = functionArgument.getDescription();
        }
    }
}
