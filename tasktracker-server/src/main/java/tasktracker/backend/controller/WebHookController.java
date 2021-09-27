package tasktracker.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tasktracker.backend.eventbus.TaskTrackerEvent;
import tasktracker.backend.eventbus.TaskTrackerEventBus;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/webhooks")
public class WebHookController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TaskTrackerEventBus eventBus;

    @PostMapping(path = "/jira")
    public ResponseEntity<?> processJiraWebHook(@RequestBody final String body) {
        log.info("Callback received from Jira: {}", body);

        parseAsReleaseEvent(body).ifPresent((event) -> eventBus.post(TaskTrackerEvent.of(event)));

        return ResponseEntity.ok().build();
    }

    private Optional<JiraWebhookEvent> parseAsReleaseEvent(final String json) {
        try {
            return Optional.ofNullable(mapper.readValue(json, JiraWebhookEvent.class));
        } catch (IOException e) {
            log.warn("Error paring JSON", e);
        }
        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraWebhookEvent {
        private Long timestamp;
        private String webhookEvent;
        private JiraReleaseVersionBody version;

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public String getWebhookEvent() {
            return webhookEvent;
        }

        public void setWebhookEvent(String webhookEvent) {
            this.webhookEvent = webhookEvent;
        }

        public JiraReleaseVersionBody getVersion() {
            return version;
        }

        public void setVersion(JiraReleaseVersionBody version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return "JiraWebhookEvent{" +
                    "timestamp=" + timestamp +
                    ", webhookEvent='" + webhookEvent + '\'' +
                    ", version=" + version +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraReleaseVersionBody {
        private String self;
        private Long id;
        private String description;
        private String name;
        private Boolean archived;
        private Boolean released;
        private Boolean overdue;
        private String userReleaseDate;
        private Long projectId;

        public String getSelf() {
            return self;
        }

        public void setSelf(String self) {
            this.self = self;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getArchived() {
            return archived;
        }

        public void setArchived(Boolean archived) {
            this.archived = archived;
        }

        public Boolean getReleased() {
            return released;
        }

        public void setReleased(Boolean released) {
            this.released = released;
        }

        public Boolean getOverdue() {
            return overdue;
        }

        public void setOverdue(Boolean overdue) {
            this.overdue = overdue;
        }

        public String getUserReleaseDate() {
            return userReleaseDate;
        }

        public void setUserReleaseDate(String userReleaseDate) {
            this.userReleaseDate = userReleaseDate;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        @Override
        public String toString() {
            return "JiraReleaseVersionBody{" +
                    "self='" + self + '\'' +
                    ", id=" + id +
                    ", description='" + description + '\'' +
                    ", name='" + name + '\'' +
                    ", archived=" + archived +
                    ", released=" + released +
                    ", overdue=" + overdue +
                    ", userReleaseDate='" + userReleaseDate + '\'' +
                    ", projectId=" + projectId +
                    '}';
        }
    }
}
