package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import tasktracker.backend.model.Incident;

import java.util.Date;

@ToString
public class IncidentModel {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("date")
    private Date date;
    @JsonProperty("url")
    private String url;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("task_id")
    private Long taskId;

    public IncidentModel(final Incident incident) {
        this.id = incident.getId();
        this.date = incident.getDate();
        this.title = incident.getTitle();
        this.description = incident.getDescription();
        this.taskId = incident.getTask().getId();
        this.url = incident.getUrl();
    }
}
