package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tasktracker.backend.model.TaskStats;

public class ColumnModel {
    @JsonProperty("column_name")
    private String columnName;
    @JsonProperty("column_type")
    private String columnType;

    public ColumnModel(String columnName, TaskStats.ColumnType columnType) {
        this.columnName = columnName;
        this.columnType = columnType.name();
    }
}
