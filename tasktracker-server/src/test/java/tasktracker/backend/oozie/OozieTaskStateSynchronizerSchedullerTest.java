package tasktracker.backend.oozie;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.service.TaskTrackerService;

import java.util.Optional;

import static tasktracker.backend.model.Task.State;

@RunWith(SpringJUnit4ClassRunner.class)
@Import({MyTestConfiguration.class})
@SpringBootTest
public class OozieTaskStateSynchronizerSchedullerTest {

    @MockBean
    private OozieService oozieService;
    @Autowired
    private TaskTrackerService taskTrackerService;
    @Autowired
    private OozieTaskStateSynchronizerScheduler synchronizer;

    @Test
    public void testSucceededOozieJobTable() {
        final Task task = new Task();
        final OozieWorkflowJob job = new OozieWorkflowJob();

        // SUCCEEDED
        job.setStatus("SUCCEEDED");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.SUCCEEDED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("SUCCEEDED");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // KILLED
        job.setStatus("KILLED");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("KILLED");
        task.setState(State.SCHEDULED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("KILLED");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // FAILED
        job.setStatus("FAILED");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("FAILED");
        task.setState(State.SUCCEEDED);
        Assert.assertEquals(State.SUCCEEDED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("FAILED");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // RUNNING
        job.setStatus("RUNNING");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("RUNNING");
        task.setState(State.SUCCEEDED);
        Assert.assertEquals(State.SUCCEEDED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("RUNNING");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // SUSPENDED
        job.setStatus("SUSPENDED");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("SUSPENDED");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("SUSPENDED");
        task.setState(State.SCHEDULED);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("SUSPENDED");
        task.setState(State.SUCCEEDED);
        Assert.assertEquals(State.SUCCEEDED, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // PREP
        job.setStatus("SUSPENDED");
        task.setState(State.SCHEDULED);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        // PREP
        job.setStatus("PREP");
        task.setState(State.SCHEDULED);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("PREP");
        task.setState(State.RUNNING);
        Assert.assertEquals(State.RUNNING, synchronizer.oozieWfJobStatusToTaskState(job, task));

        job.setStatus("PREP");
        task.setState(State.FAILED);
        Assert.assertEquals(State.FAILED, synchronizer.oozieWfJobStatusToTaskState(job, task));
    }

    // Задача считается синхронизированной, только при условии перехода в териминальное состояние
    // либо если у задачи нет Oozie Workflow Job ID (актуально для всех старых задач)

    @Test
    public void testSynchronizeTaskStateNoWfJob() throws OozieServiceException {
        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(null);
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob("1")).thenReturn(Optional.empty());

        Assert.assertTrue(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.RUNNING, task.getState());
        Assert.assertFalse(taskTrackerService.isTerminalState(task));
        Assert.assertTrue(task.getSynch());
        Assert.assertNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobSucceeded() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("SUCCEEDED");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertTrue(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.SUCCEEDED, task.getState());
        Assert.assertTrue(taskTrackerService.isTerminalState(task));
        Assert.assertTrue(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobFailed() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("FAILED");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertTrue(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.FAILED, task.getState());
        Assert.assertTrue(taskTrackerService.isTerminalState(task));
        Assert.assertTrue(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobKilled() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("KILLED");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertTrue(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.FAILED, task.getState());
        Assert.assertTrue(taskTrackerService.isTerminalState(task));
        Assert.assertTrue(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobRunning() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("RUNNING");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertFalse(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.RUNNING, task.getState());
        Assert.assertFalse(taskTrackerService.isTerminalState(task));
        Assert.assertFalse(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobSuspended() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("SUSPENDED");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertFalse(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.RUNNING, task.getState());
        Assert.assertFalse(taskTrackerService.isTerminalState(task));
        Assert.assertFalse(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }

    @Test
    public void testSynchronizeTaskStateWfJobPrep() throws OozieServiceException {
        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId("1");
        job.setStatus("PREP");

        final Project project = new Project();
        project.setName("Project");
        taskTrackerService.save(project);

        final Task task = new Task();
        task.setProject(project);
        task.setState(State.RUNNING);
        task.setSynch(false);
        task.setOozieWorkflowId(job.getId());
        taskTrackerService.save(project, task);

        Mockito.when(oozieService.findWorkflowJob(job.getId())).thenReturn(Optional.of(job));

        Assert.assertFalse(synchronizer.synchronizeTaskState(project, task));
        Assert.assertEquals(State.RUNNING, task.getState());
        Assert.assertFalse(taskTrackerService.isTerminalState(task));
        Assert.assertFalse(task.getSynch());
        Assert.assertNotNull(task.getOozieWorkflowId());
    }
}