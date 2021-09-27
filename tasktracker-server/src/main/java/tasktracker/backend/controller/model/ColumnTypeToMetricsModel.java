package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.TaskStats;

import java.util.List;
import java.util.stream.Collectors;

public class ColumnTypeToMetricsModel {
    @JsonProperty("column_type")
    private String columnType;
    @JsonProperty("allowed_metrics")
    private List<String> metrics;

    public ColumnTypeToMetricsModel(TaskStats.ColumnType columnType, List<DataQualityCondition.Metric> metrics) {
        this.columnType = columnType.name();
        this.metrics = metrics.stream()
                .map(metric -> metric.name())
                .collect(Collectors.toList());
    }
}
