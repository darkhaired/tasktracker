package tasktracker.backend.controller.model.dataqualityfrontend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FrontendDQRule {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("task_name")
    private String taskName;
    @JsonProperty("table_name")
    private String tableName;
    @JsonProperty("caption")
    private String caption = "";
    @JsonProperty("conditions")
    private List<FrontendDQCondition> conditions = Lists.newArrayList();
}
