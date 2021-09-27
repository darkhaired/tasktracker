package tasktracker.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasktracker.client.models.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TaskTrackerClient implements AutoCloseable {
    private static final int DEFAULT_TIMEOUT = 5;
    private static final String DEFAULT_PRODUCTION_URL = "http://tasktracker.dmp.vimpelcom.ru/api";
    private static final String DEFAULT_DEVELOPMENT_URL = "http://tasktracker-dev.dmp.vimpelcom.ru/api";
    private final TypeReference<Response<Project>> projectTypeReference = new TypeReference<Response<Project>>() {
    };
    private final TypeReference<Response<Task>> taskTypeReference = new TypeReference<Response<Task>>() {
    };
    private final TypeReference<Response<TaskMetric>> metricTypeReference = new TypeReference<Response<TaskMetric>>() {
    };
    private final TypeReference<Response<TaskError>> errorTypeReference = new TypeReference<Response<TaskError>>() {
    };
    private final TypeReference<Response<List<Task>>> tasksTypeReference = new TypeReference<Response<List<Task>>>() {
    };
    private final TypeReference<Response<List<Project>>> projectsTypeReference = new TypeReference<Response<List<Project>>>() {
    };
    private final TypeReference<Response<List<TaskMetric>>> metricsTypeReference = new TypeReference<Response<List<TaskMetric>>>() {
    };
    private final TypeReference<Response<List<TaskError>>> errorsTypeReference = new TypeReference<Response<List<TaskError>>>() {
    };
    private final TypeReference<TaskStatsResponse> taskStatsTypeReference = new TypeReference<TaskStatsResponse>() {
    };
    private final TypeReference<Response<Warning>> warningTypeReference = new TypeReference<Response<Warning>>() {
    };
    private final String url;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    private TaskTrackerClient(final String url, final long connectionTimeout, final long socketTimeout, final long connectionRequestTimeout) {
        Preconditions.checkArgument(url != null && !url.isEmpty(), "'url' must not be null or empty!");
        this.url = url;
        final RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout((int) (connectionTimeout * 1000))
                .setSocketTimeout((int) (socketTimeout * 1000))
                .setConnectionRequestTimeout((int) (connectionRequestTimeout * 1000))
                .build();

        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    }

    private TaskTrackerClient(final String url) {
        this(url, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    private TaskTrackerClient() {
        this(DEFAULT_PRODUCTION_URL, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    public static TaskTrackerClient production() {
        return new TaskTrackerClient(DEFAULT_PRODUCTION_URL);
    }

    public static TaskTrackerClient development() {
        return new TaskTrackerClient(DEFAULT_DEVELOPMENT_URL);
    }

    private static <R> ResponseHandler<Response<R>> responseHandler(final ObjectMapper objectMapper,
                                                                    final TypeReference<Response<R>> typeReference) {
        return httpResponse -> {
            final InputStream is = httpResponse.getEntity().getContent();
            final String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            final Response<R> response = objectMapper.readValue(json, typeReference);
            response.setStatusCode(httpResponse.getStatusLine().getStatusCode());

            return response;
        };
    }

    private static <R> ResponseHandler<R> handler(final ObjectMapper objectMapper,
                                                  final TypeReference<R> typeReference) {
        return httpResponse -> {
            final InputStream is = httpResponse.getEntity().getContent();
            final String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            System.out.println(json);
            return objectMapper.readValue(json, typeReference);
        };
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }

    public ProjectContext projectContext(final String name) {
        return new ProjectContext(this, name);
    }

    public V1 v1() {
        return new V1(this);
    }

    @Override
    public String toString() {
        return "TaskTrackerClient{" +
                "url='" + url + '\'' +
                '}';
    }

    public static class V1 {
        private final TaskTrackerClient client;

        public V1(final TaskTrackerClient client) {
            this.client = client;
        }

        public Response<List<Project>> getProjects() throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.projectsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<List<Project>> getProjectByName(final String name) throws ClientException {
            if (Objects.isNull(name)) {
                throw new IllegalArgumentException("'name' can't be null!");
            }
            try {
                final URIBuilder builder = new URIBuilder(client.url + "/v1/projects");
                builder.setParameter("name", name);
                final HttpGet getRequest = new HttpGet(builder.build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.projectsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Project> getProject(final String name) throws ClientException {
            return getProjectByName(name).map(projects -> {
                return projects.isEmpty() ? null : projects.get(0);
            });
        }

        public Response<Project> getOrCreateProject(final String name) throws ClientException {
            final Response<Project> response = getProject(name);
            if (Objects.nonNull(response.orElse(null))) {
                return response;
            }
            return createProject(Project.of().setName(name));
        }

        public Response<Project> createProject(final Project project) throws ClientException {
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity projectEntity = new StringEntity(client.objectMapper.writeValueAsString(project));
                postRequest.setEntity(projectEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.projectTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Project> getProject(final long projectId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + projectId).build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.projectTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Boolean> deleteProject(final Project project) throws ClientException {
            try {
                final HttpDelete deleteRequest = new HttpDelete(new URIBuilder(client.url + "/v1/projects/" + project.getId()).build());
                return client.httpClient.execute(deleteRequest, responseHandler(client.objectMapper, new TypeReference<Response<Boolean>>() {
                }));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<List<Task>> getTasks(final long projectId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.tasksTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<List<Task>> getTasks(final Project project) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + project.getId() + "/tasks").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.tasksTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated {@link #createTask(Project, Task)} instead.
         */
        @Deprecated
        public Response<Task> createTask(final long projectId, final Task task) throws ClientException {
            try {
                HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks").build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity projectEntity = new StringEntity(client.objectMapper.writeValueAsString(task));
                postRequest.setEntity(projectEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Task> createTask(final Project project, final Task task) throws ClientException {
            try {
                HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + project.getId() + "/tasks").build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity projectEntity = new StringEntity(client.objectMapper.writeValueAsString(task));
                postRequest.setEntity(projectEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated {@link #getTask(Project, Task)} instead
         */
        @Deprecated
        public Response<Task> getTask(final long projectId, final long taskId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId).build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Task> getTask(final Project project, final Task task) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + project.getId() + "/tasks/" + task.getId()).build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #updateTask(Project, Task)} instead
         */
        @Deprecated
        public Response<Task> updateTask(final long projectId, final long taskId, final Task task) throws ClientException {
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId).build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(task));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Task> updateTask(final Project project, final Task task) throws ClientException {
            final long projectId = project.getId();
            final long taskId = task.getId();
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId).build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(task));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.taskTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #getMetrics(Project, Task)} instead
         */
        @Deprecated
        public Response<List<TaskMetric>> getMetrics(final long projectId, final long taskId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/metrics").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.metricsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<List<TaskMetric>> getMetrics(final Project project, final Task task) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + project.getId() + "/tasks/" + task.getId() + "/metrics").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.metricsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #createMetric(Project, Task, TaskMetric)} instead
         */
        @Deprecated
        public Response<TaskMetric> createMetric(final long projectId, final long taskId, final TaskMetric taskMetric) throws ClientException {
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/metrics").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(taskMetric));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.metricTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<TaskMetric> createMetric(final Project project, final Task task, final TaskMetric taskMetric) throws ClientException {
            final long projectId = project.getId();
            final long taskId = task.getId();
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/metrics").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(taskMetric));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.metricTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #getMetric(Project, Task, TaskMetric)} instead
         */
        @Deprecated
        public Response<TaskMetric> getMetric(final long projectId, final long taskId, final long metricId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(
                        new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/metrics/" + metricId).build()
                );
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.metricTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<TaskMetric> getMetric(final Project project, final Task task, final TaskMetric metric) throws ClientException {
            final long projectId = project.getId();
            final long taskId = task.getId();
            final long metricId = metric.getId();
            try {
                final HttpGet getRequest = new HttpGet(
                        new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/metrics/" + metricId).build()
                );
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.metricTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #getErrors(Project, Task)} instead
         */
        @Deprecated
        public Response<List<TaskError>> getErrors(final long projectId, final long taskId) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/errors").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.errorsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<List<TaskError>> getErrors(final Project project, final Task task) throws ClientException {
            try {
                final HttpGet getRequest = new HttpGet(new URIBuilder(client.url + "/v1/projects/" + project.getId() + "/tasks/" + task.getId() + "/errors").build());
                return client.httpClient.execute(getRequest, responseHandler(client.objectMapper, client.errorsTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        /**
         * @deprecated use {@link #createError(Project, Task, TaskError)} instead
         */
        @Deprecated
        public Response<TaskError> createError(final long projectId, final long taskId, final TaskError taskError) throws ClientException {
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/errors").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(taskError));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.errorTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<TaskError> createError(final Project project, final Task task, final TaskError taskError) throws ClientException {
            final long projectId = project.getId();
            final long taskId = task.getId();
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/errors").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(taskError));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.errorTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        private Map<String, List<TaskStats>> toStatisticsBody(final List<TaskStats> stats) {
            return Collections.singletonMap("statistics", stats);
        }

        public List<TaskStats> createStats(final Project project, final Task task, final List<TaskStats> stats) throws ClientException {
            try {
                HttpPost postRequest = new HttpPost(
                        new URIBuilder(client.url + "/v2/projects/" + project.getId() + "/tasks/" + task.getId() + "/statistics").build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity projectEntity = new StringEntity(client.objectMapper.writeValueAsString(toStatisticsBody(stats)));
                postRequest.setEntity(projectEntity);
                return client.httpClient.execute(postRequest, handler(client.objectMapper, client.taskStatsTypeReference)).getStatistics();
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public List<TaskStats> createStats(final Project project, final Task task, final TaskStats stats) throws ClientException {
            try {
                HttpPost postRequest = new HttpPost(
                        new URIBuilder(client.url + "/v2/projects/" + project.getId() + "/tasks/" + task.getId() + "/statistics").build());
                postRequest.setHeader("Content-type", "application/json");
                StringEntity projectEntity = new StringEntity(client.objectMapper.writeValueAsString(toStatisticsBody(Lists.newArrayList(stats))));
                postRequest.setEntity(projectEntity);
                return client.httpClient.execute(postRequest, handler(client.objectMapper, client.taskStatsTypeReference)).getStatistics();
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }

        public Response<Warning> createWarning(final Project project, final Task task, final Warning warning) throws ClientException {
            final long projectId = project.getId();
            final long taskId = task.getId();
            try {
                final HttpPost postRequest = new HttpPost(new URIBuilder(client.url + "/v1/projects/" + projectId + "/tasks/" + taskId + "/warnings").build());
                postRequest.setHeader("Content-type", "application/json");
                final StringEntity taskEntity = new StringEntity(client.objectMapper.writeValueAsString(warning));
                postRequest.setEntity(taskEntity);
                return client.httpClient.execute(postRequest, responseHandler(client.objectMapper, client.warningTypeReference));
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }
    }

    public static class ProjectContext {
        private static final Logger logger = LogManager.getLogger(ProjectContext.class);
        private final TaskTrackerClient client;
        private final Project project;

        private Task task = null;

        private ProjectContext(final TaskTrackerClient client, final String projectName) {
            this.client = client;
            Project project = null;
            try {
                project = client.v1().getOrCreateProject(projectName).orElse(Project.of().setName(projectName));
            } catch (ClientException e) {
                logger.error("An error occurred while getOrCreateProject", e);
            }

            this.project = project;
        }

        public synchronized Project getProject() {
            return project;
        }

        public synchronized Task getTask() {
            return task;
        }

        public synchronized ProjectContext getTask(final long id) {
            try {
                this.task = client.v1().getTask(this.project, Task.of(id)).orElse(null);
                if (Objects.isNull(this.task)) {
                    logger.warn("Project task not found by id='{}'", id);
                }
            } catch (ClientException e) {
                logger.error("An error occurred while getTask", e);
            }

            return this;
        }

        public synchronized ProjectContext newTask(final Task task) {
            try {
                if (Objects.nonNull(this.task)) {
                    return this;
                }
                if (Objects.isNull(task.getStatus())) {
                    task.setStatus(Task.Status.DISABLE);
                }
                if (Objects.isNull(task.getStartDate())) {
                    task.setStartDate(new Date());
                }
                if (Objects.isNull(task.getUser())) {
                    task.setUser("unknown");
                }
                this.task = client.v1().createTask(this.project, task).orElse(null);
            } catch (ClientException e) {
                logger.error("An error occurred while newTask", e);
            }

            return this;
        }

        public synchronized ProjectContext runTask() {
            try {
                task = client.v1().updateTask(this.project, this.task.setStatus(Task.Status.RUNNING).setEndDate(new Date())).orElse(null);
            } catch (ClientException e) {
                logger.error("An error occurred while runTask", e);
            }

            return this;
        }

        public synchronized ProjectContext finishedTask() {
            try {
                task = client.v1().updateTask(this.project, this.task.setStatus(Task.Status.SUCCEEDED).setEndDate(new Date())).orElse(null);
            } catch (ClientException e) {
                logger.error("An error occurred while finishedTask", e);
            }

            return this;
        }

        public synchronized ProjectContext failedTask() {
            try {
                task = client.v1().updateTask(this.project, this.task.setStatus(Task.Status.FAILED).setEndDate(new Date())).orElse(null);
            } catch (ClientException e) {
                logger.error("An error occurred while failedTask", e);
            }

            return this;
        }

        public synchronized ProjectContext newMetric(final TaskMetric metric) {
            try {
                client.v1().createMetric(this.project, this.task, metric);
            } catch (ClientException e) {
                logger.error("An error occurred while newMetric", e);
            }

            return this;
        }

        public void dispose() {
            // does nothing
        }

        @Override
        public String toString() {
            return "ProjectContext{" +
                    "project=" + project +
                    ", task=" + task +
                    '}';
        }
    }
}