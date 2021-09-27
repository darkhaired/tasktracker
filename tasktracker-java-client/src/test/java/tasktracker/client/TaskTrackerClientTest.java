package tasktracker.client;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import tasktracker.client.models.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Ignore
public class TaskTrackerClientTest {

    private static final String projectName = "TestProject";
    private static TaskTrackerClient client;

    @BeforeClass
    public static void setUp() throws ClientException {
        client = TaskTrackerClient.development();

//        final Project project = new Project();
//        project.setName(projectName);

        final Response<Project> response = client.v1().getOrCreateProject(projectName);
        System.out.println(response);

        Assert.assertNotNull(response);
//        Assert.assertTrue(response.isSuccess());
    }

    //    @AfterClass
    public static void tearDown() throws IOException, ClientException {
        /*client.v1().getProjects().forEach(projects -> {
            projects.forEach(project -> {
                try {
                    System.out.println("delete project: " + project);
                    System.out.println(client.v1().deleteProject(project));
                } catch (ClientException e) {
                    e.printStackTrace();
                }
            });
        });*/
        Project project = client.v1().getOrCreateProject(projectName).get();
        client.v1().deleteProject(project);
        System.out.println("tearDown");

        final Response<List<Project>> projects = client.v1().getProjects();
        System.out.println("projects = " + projects.get());
        /*project = client.v1().getOrCreateProject("Booo").get();
        client.v1().deleteProject(project);
        project = client.v1().getOrCreateProject("ExampleProject").get();
        client.v1().deleteProject(project);*/
        client.close();
    }

    public static Date getDateTimeFromFormattedString(String inputDate) {
        if (Objects.isNull(inputDate)) {
            return null;
        }

        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        df.setLenient(false);

        try {
            return df.parse(inputDate);
        } catch (ParseException ex) {
            return null;
        }
    }

    @Test
    @Ignore
    public void getProjects() throws ClientException {
        final Response<List<Project>> projects = client.v1().getProjects();

        Assert.assertNotNull(projects);
        Assert.assertTrue(projects.isSuccess());
    }

    @Test
    @Ignore
    public void createMetric() throws ClientException {
        final List<Project> projects = client.v1().getProjectByName(projectName).get();

        final Project project = projects.get(0);
        System.out.println("project = " + project);

        final Task task = new Task();
        task.setName("first_task");
        task.setStartDate(new Date());
        task.setUser("alvilitvinov");
        task.setStatus(Task.Status.RUNNING);

        final Long taskId = client.v1().createTask(project.getId(), task).map(Task::getId).get();

        final TaskMetric metric = new TaskMetric();
        metric.setName("first_metric");
        metric.setValue("Hello, World");

        final Response<TaskMetric> response = client.v1().createMetric(project.getId(), taskId, metric);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());

    }

    @Test
    @Ignore
    public void createWarning() throws ClientException {
        final List<Project> projects = client.v1().getProjectByName(projectName).get();

        final Project project = projects.get(0);
        System.out.println("project = " + project);

        Task task = new Task();
        task.setName("FirstTask");
        task.setStartDate(new Date());
        task.setNominalDate(new Date());
        task.setUser("fkarabaeva");
        task.setStatus(Task.Status.SUCCEEDED);

        task = client.v1().createTask(project.getId(), task).get();
        System.out.println("task = " + task);

        final Warning warning = new Warning();
        warning.setMessage("test warning");

        final Response<Warning> response = client.v1().createWarning(project, task, warning);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());

    }

    @Test
    @Ignore
    public void createStats() throws ClientException {
        final List<Project> projects = client.v1().getProjectByName(projectName).get();

        final Project project = projects.get(0);
        System.out.println("project = " + project);

        Task task1 = new Task();
        task1.setName("TestTask1");
        task1.setStartDate(getDateTimeFromFormattedString("2020-07-20T08:00:00.000+0000"));
        task1.setNominalDate(getDateTimeFromFormattedString("2020-07-01T08:00:00.000+0000"));
        task1.setUser("fkarabaeva");
        task1.setStatus(Task.Status.SUCCEEDED);

        task1 = client.v1().createTask(project, task1).get();
        System.out.println("task = " + task1);

        TaskStats stats1 = new TaskStats();
        stats1.setColumn("stg.stg_task_1.row_cnt");
        stats1.setCount(40L);
        stats1.setTotalCount(100L);
        client.v1().createStats(project, task1, stats1);

        Task task12 = new Task();
        task12.setName("TestTask1");
        task12.setStartDate(getDateTimeFromFormattedString("2020-07-20T08:00:00.000+0000"));
        task12.setNominalDate(getDateTimeFromFormattedString("2020-07-02T08:00:00.000+0000"));
        task12.setUser("fkarabaeva");
        task12.setStatus(Task.Status.SUCCEEDED);

        task12 = client.v1().createTask(project, task12).get();
        System.out.println("task = " + task12);

        TaskStats stats2 = new TaskStats();
        stats2.setColumn("stg.stg_task_1.row_cnt");
        stats2.setCount(60L);
        stats2.setTotalCount(100L);
        client.v1().createStats(project, task12, stats2);

        Task task3 = new Task();
        task3.setName("TestTask1");
        task3.setStartDate(getDateTimeFromFormattedString("2020-07-20T08:00:00.000+0000"));
        task3.setNominalDate(getDateTimeFromFormattedString("2020-07-03T08:00:00.000+0000"));
        task3.setUser("fkarabaeva");
        task3.setStatus(Task.Status.SUCCEEDED);

        task3 = client.v1().createTask(project, task3).get();
        System.out.println("task = " + task3);

        TaskStats stats3 = new TaskStats();
        stats3.setColumn("stg.stg_task_1.row_cnt");
        stats3.setCount(50L);
        stats3.setTotalCount(100L);
        client.v1().createStats(project, task3, stats3);


    }

    @Test
    @Ignore
    public void createError() throws ClientException {
        final TaskTrackerClient client = TaskTrackerClient.development();
//        final List<Project> projects = client.v1().getOrCreateProject("Test").get();
        final Project project = client.v1().getOrCreateProject(projectName).get();

        final Task task = new Task();
        task.setName("baraboom");
        task.setStartDate(new Date());
        task.setUser("alvilitvinov");
        task.setStatus(Task.Status.CANCELED);

        final Long taskId = client.v1().createTask(project, task).map(Task::getId).get();

        final TaskError error = new TaskError();
        error.setType(TaskError.ErrorType.DATA_LOAD_ERROR);
        error.setReason("Test Java Client");

        final Response<TaskError> response = client.v1().createError(project.getId(), taskId, error);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());
    }

    @Test
    @Ignore
    public void createTask() throws ClientException {
        final List<Project> projects = client.v1().getProjectByName(projectName).get();

        final Project project = projects.get(0);

        final Date nominalDate = Date.from(LocalDateTime.of(2019, 12, 1, 12, 0).toInstant(ZoneOffset.UTC));
        final Task task = new Task();
        task.setName("first_task");
        task.setStartDate(new Date());
        task.setNominalDate(nominalDate);

        task.setUser("alvilitvinov");
        task.setStatus(Task.Status.RUNNING);

        final Response<Task> response = client.v1().createTask(project.getId(), task);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());

    }

    @Test
    @Ignore
    public void example() throws ClientException {
        final Project project = client.v1().getOrCreateProject(projectName).get();

        // Создаем задачу и устанавливаем статус RUNNING
        final Date nominalDate = Date.from(LocalDateTime.of(2019, 12, 1, 12, 0).toInstant(ZoneOffset.UTC));

        final Response<Task> response = client.v1().createTask(project, Task.of().setName("MainTask").setStartDate(new Date()).setStatus(Task.Status.RUNNING).setNominalDate(nominalDate));
        System.out.println(response);
        final Task task = response.get();

        // В конце выполнения программ, ранее созданную задачу нужно обновить и установить статус соответствующий тому как программа завершилась

        final Task updatedTask = client.v1().updateTask(project, task.setEndDate(new Date()).setStatus(Task.Status.SUCCEEDED)).get();

        System.out.println(updatedTask);
    }

    @Test
    @Ignore
    public void example2() {
        final Date nominalDate = Date.from(LocalDateTime.of(2019, 12, 1, 12, 0).toInstant(ZoneOffset.UTC));
        final TaskTrackerClient.ProjectContext context = TaskTrackerClient
                .development()
                .projectContext(projectName)
                .newTask(Task.of().setName("MainTask2").setStartDate(new Date()).setStatus(Task.Status.RUNNING).setNominalDate(nominalDate))
                .runTask()
                .newMetric(TaskMetric.of().setName("count").setValue(990))
                .finishedTask();
        System.out.println(context);
    }
}
