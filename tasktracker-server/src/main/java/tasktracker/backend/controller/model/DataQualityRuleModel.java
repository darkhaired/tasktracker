package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import tasktracker.backend.model.DataQualityRule;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
public class DataQualityRuleModel {
    @JsonProperty("id")
    private Long id;
    @NotBlank(message = "Field 'task_name' should not be blank")
    @JsonProperty("task_name")
    private String taskName;
    @NotBlank(message = "Field 'table_name' should not be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+$")
    @JsonProperty("table_name")
    private String tableName;
    @NotBlank(message = "Field 'caption' should not be blank")
    @JsonProperty("caption")
    private String caption = "";
    @NotEmpty(message = "Field 'conditions' should not be empty")
    @JsonProperty("conditions")
    private List<DataQualityConditionModel> conditions = Lists.newArrayList();

    public DataQualityRuleModel(final DataQualityRule rule) {
        this.id = rule.getId();
        this.taskName = rule.getTaskName();
        this.tableName = rule.getTableName();
        this.caption = rule.getCaption();
        this.conditions = rule.getConditions()
                .stream()
                .map(DataQualityConditionModel::new)
                .collect(Collectors.toList());
    }

    public DataQualityRule to() {
        DataQualityRule rule = new DataQualityRule();
        rule.setId(id);
        rule.setCaption(caption);
        rule.setTableName(StringUtils.trim(tableName));
        rule.setTaskName(StringUtils.trim(taskName));
        rule.setConditions(conditions
                .stream()
                .map(DataQualityConditionModel::to)
                .collect(Collectors.toList()));
        return rule;
    }
}
