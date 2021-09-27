package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskMetric {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    @Accessors(chain = true)
    private String name;
    @JsonProperty("value")
    private String value;
    @JsonProperty("task_id")
    private Long taskId;
    @JsonProperty("label")
    private String label;

    public TaskMetric(final String name, final Object value) {
        this(name, value, null);
    }

    public TaskMetric(final String name, final Object value, final String label) {
        this.name = name;
        this.value = String.valueOf(value);
        this.label = label;
    }

    public static TaskMetric of() {
        return new TaskMetric();
    }

    /**
     * @deprecated Please, use constructor!
     */
    @Deprecated
    public static TaskMetric create(final String name, final Object value) {
        Preconditions.checkArgument(name != null && !name.isEmpty(), "'name' must not be null or empty!");
        final TaskMetric metric = new TaskMetric();
        metric.setName(name);
        metric.setValue(value);

        return metric;
    }

    public TaskMetric setName(final String name) {
        this.name = name;
        return this;
    }

    public TaskMetric setValue(final Object value) {
        this.value = Objects.toString(value);
        return this;
    }
}
