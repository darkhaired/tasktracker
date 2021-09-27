package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import tasktracker.backend.model.TaskMetric;

import java.util.Date;

@ToString(of = {"id", "name", "value", "label", "taskId"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskMetricModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("label")
    private String label;
    @JsonProperty("value")
    private String value;
    @JsonProperty("task_id")
    private Long taskId;
    @JsonProperty("task_start_date")
    private Date taskStartDate;
    @JsonProperty("task_end_date")
    private Date taskEndDate;

    public TaskMetricModel(final TaskMetric metric) {
        this.id = metric.getId();
        this.name = metric.getName();
        this.label = metric.getLabel();
        this.value = metric.getValue2();
        this.taskId = metric.getId();
        this.taskStartDate = metric.getTask().getStartDate();
        this.taskEndDate = metric.getTask().getEndDate();
    }

    public TaskMetric toTaskMetric() {
        final TaskMetric metric = new TaskMetric();
        metric.setId(id);
        metric.setName(name);
        metric.setLabel(label);
        metric.setValue2(value);

        return metric;
    }
}
