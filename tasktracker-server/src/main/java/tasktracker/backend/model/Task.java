package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"applicationId"})
@Entity(name = "TaskState")
@Table(
        name = "task_state",
        indexes = {
                @Index(columnList = "project_id,name", name = "task_state_index_on_projectid_name"),
                @Index(columnList = "project_id,nominal_date", name = "task_state_index_on_projectid_nominaldate"),
                @Index(columnList = "project_id, start_date,end_date", name = "task_state_index_on_projectid_startdate_enddate")
        })
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "application_id")
    private String applicationId;
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private State state;
    @Column(name = "name")
    private String name;
    @Column(name = "start_date")
    private Date startDate = new Date();
    @Column(name = "end_date")
    private Date endDate;
    @Column(name = "username")
    private String user;
    @Column(name = "timestamp")
    private long timestamp = System.currentTimeMillis();
    @Column(name = "auto_updated")
    private Boolean autoUpdated = false;
    @Transient
    private Task child;
    @Column(name = "nominal_date")
    private Date nominalDate;
    @Column(name = "next_date")
    private Date nextDate;
    @OneToOne
    @JoinColumn(name = "task_type")
    private TaskType type;
    @Column(name = "oozie_workflow_name")
    private String oozieWorkflowName;
    @Column(name = "oozie_workflow_id")
    private String oozieWorkflowId;
    @Column(name = "oozie_coordinator_id")
    private String oozieCoordinatorId;
    /**
     * Service field.
     * Set is true if the state of the task is synchronized with
     * the state of the essence of the external system.
     * For example: Oozie workflow job
     */
    @Column(name = "synchronized", nullable = false, columnDefinition = "boolean default false")
    private Boolean synch = false;
    /**
     * DQ service field
     */
    @Column(name = "analyzed")
    private Boolean analyzed = false;

    @Transient
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    @Transient
    public boolean isFailed() {
        return state == State.FAILED;
    }

    @Transient
    public boolean isCompleted() {
        return state == State.SUCCEEDED;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", applicationId='" + applicationId + '\'' +
                ", project=" + project +
                ", state=" + state +
                ", name='" + name + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", user='" + user + '\'' +
                ", nominalDate=" + nominalDate +
                ", nextDate=" + nextDate +
                ", oozieWorkflowId='" + oozieWorkflowId + '\'' +
                ", oozieCoordinatorId='" + oozieCoordinatorId + '\'' +
                '}';
    }

    public enum State {
        RUNNING,
        SCHEDULED,
        FAILED,
        CANCELED,
        SUCCEEDED;

        public static State findByName(final String name) {
            for (final State state : State.values()) {
                if (state.name().equalsIgnoreCase(name)) {
                    return state;
                }
            }
            throw new RuntimeException("Unknown state name '" + name + "'");
        }
    }

    public enum DateType {
        NOMINAL_DATE,
        START_DATE,
        END_DATE,
        PERIOD
    }
}
