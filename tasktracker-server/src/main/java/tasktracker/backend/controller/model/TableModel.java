package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableModel {
    @JsonProperty("table")
    private String tableName;
    @JsonProperty("task_name")
    private String taskName;
    @JsonProperty("columns")
    private List<ColumnModel> columnModels;
}
