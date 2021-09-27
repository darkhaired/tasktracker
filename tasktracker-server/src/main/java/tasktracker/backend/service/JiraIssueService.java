package tasktracker.backend.service;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tasktracker.backend.model.Configuration;
import tasktracker.backend.model.Incident;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskError;
import tasktracker.backend.repository.IncidentRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class JiraIssueService implements IncidentService {

    private static final String DEFAULT_JIRA_ISSUE_URL = "https://servicedesk.veon.com/browse/";
    private static final String JIRA_ISSUE_SUMMARY_PREFIX = "jira.issue.summary.prefix";
    private static final String JIRA_PROJECT_NAME = "jira.project.name";
    private static final String JIRA_ISSUE_TYPE_ID = "jira.issue.type.id";
    private static final String JIRA_COMPONENT_NAME = "jira.component.name";
    private static final String JIRA_PRIORITY_ID = "jira.priority.id";
    private static final String JIRA_ASSIGNEE_NAME = "jira.assignee.name";
    private static final String JIRA_ISSUE_URL = "jira.issue.url";
    private static final String JIRA_SERVER_URI = "jira.server.uri";
    private static final String JIRA_USERNAME = "jira.username";
    private static final String JIRA_PASSWORD = "jira.password";

    private final ConfigurationService configuration;
    private final IncidentRepository repository;
    private final TaskTrackerService taskTrackerService;

    @Autowired
    public JiraIssueService(
            final ConfigurationService configuration,
            final IncidentRepository repository,
            final TaskTrackerService taskTrackerService
    ) {
        this.configuration = configuration;
        this.repository = repository;
        this.taskTrackerService = taskTrackerService;
    }


    private Optional<IssueType> getIssueType(final Project project, final int issueType) {
        return StreamSupport
                .stream(project.getIssueTypes().spliterator(), false)
                .filter(type -> type.getId() == issueType)
                .findFirst();
    }

    private static Optional<BasicComponent> getComponent(final Project project, final String componentName) {
        return StreamSupport
                .stream(project.getComponents().spliterator(), false)
                .filter(component -> component.getName().equalsIgnoreCase(componentName))
                .findFirst();
    }

    private Optional<? extends BasicPriority> getPriority(final JiraRestClient client, final int priorityId)
            throws InterruptedException, ExecutionException, TimeoutException {
        return StreamSupport
                .stream(client.getMetadataClient().getPriorities().get(1, TimeUnit.MINUTES).spliterator(), false)
                .filter(priority -> Objects.nonNull(priority.getId()) && priority.getId() == priorityId)
                .findFirst();
    }

    private String getSummary(final Task task) {
        final String prefix = configuration.findByKey(JIRA_ISSUE_SUMMARY_PREFIX).map(Configuration::getValue).orElse("");
        return prefix + task.getName();
    }

    private String getDescription(final Task task) {
        final List<TaskError> errors = taskTrackerService.findErrors(task);
        final StringBuilder builder = new StringBuilder();
        builder.append("*USER:* ").append(task.getUser()).append("\n");
        builder.append("*TASK ID:* ").append(task.getId()).append("\n");
        builder.append("*TASK START DATE:* ").append(task.getStartDate()).append("\n");
        builder.append("*TASK END DATE:* ").append(task.getEndDate()).append("\n");
        builder.append("*APPLICATION ID:* ").append(task.getApplicationId()).append("\n");
        builder.append("*ERRORS ID:* ").append(task.getApplicationId()).append("\n");
        builder.append("{code}");
        for (final TaskError error : errors) {
            builder.append(error.getReason()).append("\n");
        }
        builder.append("{code}");
        return builder.toString();
    }

    private BasicIssue createJiraIssue(final Task task) {
        try {
            final JiraRestClient client = getClient();

            final String projectName = configuration.findByKey(JIRA_PROJECT_NAME).map(Configuration::getValue).orElse("TEST_PROJECT");
            final String issueTypeId = configuration.findByKey(JIRA_ISSUE_TYPE_ID).map(Configuration::getValue).orElse(String.valueOf(1));
            final String componentName = configuration.findByKey(JIRA_COMPONENT_NAME).map(Configuration::getValue).orElse("TEST_COMPONENT");
            final String priorityId = configuration.findByKey(JIRA_PRIORITY_ID).map(Configuration::getValue).orElse(String.valueOf(1));
            final String assigneeName = configuration.findByKey(JIRA_ASSIGNEE_NAME).map(Configuration::getValue).orElse(null);

            final Project project = client.getProjectClient().getProject(projectName).claim();

            final IssueType issueType = getIssueType(project, Integer.valueOf(issueTypeId)).orElse(null);
            final BasicComponent component = getComponent(project, componentName).orElse(null);
            final BasicPriority priority = getPriority(client, Integer.valueOf(priorityId)).orElse(null);

            final String summary = getSummary(task);
            final String description = getDescription(task);


            final IssueInput issueInput = new IssueInputBuilder()
                    .setSummary(summary)
                    .setDescription(description)
                    .setIssueType(issueType)
                    .setComponents(component)
                    .setPriority(priority)
                    .setProject(project)
                    .setDueDate(new DateTime().plusDays(1))
                    .setAssigneeName(assigneeName)
                    .build();

            return client.getIssueClient().createIssue(issueInput).claim();
        } catch (Exception e) {
            throw new RuntimeException("Could not create Jira issue for Task '" + task.getId() + "'", e);
        }
    }

    @Override
    public Incident save(final Incident incident) {
        return repository.save(incident);
    }

    @Override
    public Incident create(final Task task) {
        log.info("Create incident: {}", task);
        final BasicIssue jiraIssue = createJiraIssue(task);

        final Incident incident = new Incident();
        incident.setTask(task);
        incident.setDate(new Date());
        incident.setTitle(getSummary(task));
        incident.setDescription(getDescription(task));
        incident.setUrl(DEFAULT_JIRA_ISSUE_URL + jiraIssue.getKey());

        final Incident result = save(incident);

        log.info("Incident: {}", result);

        return result;
    }

    @Override
    public List<Incident> findIncidents(final Date date) {
        return repository.findIncidents(date);
    }

    @Override
    public List<Incident> findIncidents(final tasktracker.backend.model.Project project) {
        return repository.findIncidents(project);
    }

    @Override
    public List<Incident> findIncidents(final tasktracker.backend.model.Project project, final Date date) {
        return repository.findIncidents(project, date);
    }

    @Override
    public List<Incident> findIncidents(final tasktracker.backend.model.Project project, final Date start, final Date end) {
        return Collections.emptyList();
    }

    @Override
    public List<Incident> findAll() {
        return repository.findAll();
    }

    @Override
    public void delete(final Incident incident) {
        repository.delete(incident);
    }

    @Override
    public Optional<Incident> findIncidentForTask(final Task task) {
        return repository.findByTaskId(task.getId());
    }

    public JiraRestClient getClient() {
        try {
            final URI uri = configuration.findByKey(JIRA_SERVER_URI).map(config -> {
                try {
                    return new URI(config.getValue());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }).orElseThrow(null);

            final String username = configuration.findByKey(JIRA_USERNAME).map(Configuration::getValue).orElse(null);
            final String password = configuration.findByKey(JIRA_PASSWORD).map(Configuration::getValue).orElse(null);

            final JiraRestClient client =  new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
                    uri,
                    username,
                    password
            );

            return client;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    static class User {
        public void foo() {
            System.out.println("Hello, World");
        }

        public String toString() {
            foo();
            return null;
        }
    }

    public static void main(String[] args) {
        User user = new User() {
            public void foo() {
                System.out.println("Hello, World!!!!!");
            }
        };

        System.out.println(user.getClass().getCanonicalName());
    }
}
