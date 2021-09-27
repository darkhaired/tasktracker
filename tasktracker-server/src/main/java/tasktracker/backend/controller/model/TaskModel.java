package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskType;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static tasktracker.backend.controller.DateTimePatterns.getDateTimeFormat;
import static tasktracker.backend.controller.DateTimePatterns.getDateTimeFromFormattedString;

@ToString
@NoArgsConstructor
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskModel {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("application_id")
    private String applicationId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("status")
    private String status;
    @JsonProperty("timestamp")
    private long timestamp;
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("user")
    private String user;
    @JsonProperty("metrics")
    private List<TaskMetricModel> metrics = Collections.emptyList();
    @JsonProperty("statistics")
    private List<TaskStatsResponse> statistics = Collections.emptyList();
    @JsonProperty("errors")
    private List<TaskErrorModel> errors = Collections.emptyList();
    @JsonProperty("warnings")
    private List<WarningModel> warnings = Collections.emptyList();
    @JsonProperty("project_id")
    private Long projectId;
    @JsonProperty("nominal_date")
    private String nominalDate;
    @JsonProperty("next_date")
    private String nextDate;
    @JsonProperty("type")
    private String type;

    @JsonProperty("oozie_workflow_name")
    private String oozieWorkflowName;
    @JsonProperty("oozie_workflow_id")
    private String oozieWorkflowId;
    @JsonProperty("oozie_coordinator_id")
    private String oozieCoordinatorId;
    @JsonProperty("synchronized")
    private Boolean synch;
    @JsonProperty("analyzed")
    private Boolean analyzed;

    public TaskModel(final SimpleDateFormat dateTimeFormat, final Task task) {
        this.id = task.getId();
        this.applicationId = task.getApplicationId();
        this.name = task.getName();
        this.status = task.getState().name();
        this.timestamp = task.getTimestamp();
        this.startDate = task.getStartDate() == null ? null : dateTimeFormat.format(task.getStartDate());
        this.endDate = task.getEndDate() == null ? null : dateTimeFormat.format(task.getEndDate());
        this.user = task.getUser();
        this.projectId = task.getProject().getId();
        this.nominalDate = Objects.isNull(task.getNominalDate()) ? null : dateTimeFormat.format(task.getNominalDate());
        this.nextDate = Objects.isNull(task.getNextDate()) ? null : dateTimeFormat.format(task.getNextDate());
        this.type = task.getType() == null ? null : task.getType().getName();
        this.oozieWorkflowName = task.getOozieWorkflowName();
        this.oozieWorkflowId = task.getOozieWorkflowId();
        this.oozieCoordinatorId = task.getOozieCoordinatorId();
        this.synch = task.getSynch();
        this.analyzed = task.getAnalyzed();
    }

    public TaskModel(final Task task) {
        this(getDateTimeFormat(), task);
    }


    @JsonIgnore
    public Task toTaskState() {
        final Task task = new Task();
        task.setId(id);
        task.setApplicationId(applicationId);
        task.setName(name);
        task.setState(Task.State.valueOf(status.toUpperCase().trim()));
        task.setTimestamp(timestamp);
        task.setStartDate(startDate == null ? null : getDateTimeFromFormattedString(startDate));
        task.setEndDate(endDate == null ? null : getDateTimeFromFormattedString(endDate));
        task.setUser(user);
        task.setNominalDate(Objects.isNull(nominalDate) ? null : getDateTimeFromFormattedString(nominalDate));
        task.setNextDate(Objects.isNull(nextDate) ? null : getDateTimeFromFormattedString(nextDate));
        final TaskType taskType = new TaskType();
        taskType.setName(type);
        task.setType(type == null ? null : taskType);

        task.setOozieWorkflowName(oozieWorkflowName);
        task.setOozieWorkflowId(oozieWorkflowId);

        return task;
    }
}
