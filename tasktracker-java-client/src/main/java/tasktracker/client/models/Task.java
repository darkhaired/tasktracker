package tasktracker.client.models;


import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;


@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"applicationId"})
@ToString(of = {"id", "applicationId", "name", "status", "startDate",
        "endDate", "user", "projectId", "nominalDate", "oozieWorkflowName", "oozieWorkflowId"})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {

    public enum Status {

        RUNNING("RUNNING"),
        SUCCEEDED("SUCCEEDED"),
        FAILED("FAILED"),
        SKIPPED("SKIPPED"),
        DISABLE("DISABLE"),
        CANCELED("CANCELED"),
        KILLED("KILLED");

        private final String value;
        Status(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    @JsonProperty("id")
    private Long id;
    @JsonProperty("application_id")
    @Accessors(chain = true)
    private String applicationId;
    @JsonProperty("name")
    @Accessors(chain = true)
    private String name;
    @JsonProperty("status")
    @Accessors(chain = true)
    private Status status;
    @JsonProperty("timestamp")
    @Accessors(chain = true)
    private long timestamp;
    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @Accessors(chain = true)
    private Date startDate;
    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @Accessors(chain = true)
    private Date endDate;
    @JsonProperty("user")
    @Accessors(chain = true)
    private String user;
    @JsonProperty("project_id")
    @Accessors(chain = true)
    private Long projectId;
    @JsonProperty("metrics")
    @Accessors(chain = true)
    private List<TaskMetric> metrics;
    @JsonProperty("nominal_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @Accessors(chain = true)
    private Date nominalDate;
    @JsonProperty("statistics")
    @Accessors(chain = true)
    private List<TaskStats> statistics;

    @JsonProperty("oozie_workflow_name")
    @Accessors(chain = true)
    private String oozieWorkflowName;
    @JsonProperty("oozie_workflow_id")
    @Accessors(chain = true)
    private String oozieWorkflowId;

    public Task(final long id) {
        this.id = id;
    }

    public static Task of() {
        return new Task();
    }

    public static Task of(final long id) {
        return new Task(id);
    }
}
