package tasktracker.backend.oozie;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.Task.State;
import tasktracker.backend.service.TaskTrackerService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OozieTaskStateSynchronizerScheduler {
    private static final long SYNCHRONIZATION_TASKS_PERIOD_IN_MINUTES = 2;

    private final TaskTrackerService taskTrackerService;
    private final OozieService oozieService;

    @Scheduled(fixedRate = 1000 * 60 * SYNCHRONIZATION_TASKS_PERIOD_IN_MINUTES)
    public void sync() {
        log.info("Tasks synchronization process started");

        final List<Project> projects = taskTrackerService.findProjects()
                .stream()
                .filter(project -> project.getSettings()
                        .getTaskSynchronizationAllowed())
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            log.info("Nothing to sync");
        }

        for (final Project project : projects) {

            final List<Task> unsynchronizedTasks = taskTrackerService.findUnsynchronizedTasks(project);

            log.info("{} has total non-synchronized tasks {}", project, unsynchronizedTasks.size());

            if (unsynchronizedTasks.isEmpty()) {
                log.info("Nothing to sync");
                continue;
            }

            final long synchronizedTasksCount = unsynchronizedTasks
                    .stream()
                    .map(task -> synchronizeTaskState(project, task))
                    .filter(isSync -> isSync)
                    .count();

            log.info("{} tasks was synchronized", synchronizedTasksCount);
        }

        log.info(
                "Tasks synchronization process completed. Next start in {} minutes",
                SYNCHRONIZATION_TASKS_PERIOD_IN_MINUTES
        );
    }

    public State oozieWfJobStatusToTaskState(final OozieWorkflowJob job, final Task task) {
        final String status = job.getStatus();
        final State state = task.getState();

        Preconditions.checkState(state != null, "Task state must not be null!");

        if ("SUCCEEDED".equalsIgnoreCase(status)) {
            if (state == State.RUNNING) {
                return State.SUCCEEDED;
            }
            return state;
        }

        if ("KILLED".equalsIgnoreCase(status)) {
            return State.FAILED;
        }

        if ("FAILED".equalsIgnoreCase(status)) {
            if (state == State.RUNNING) {
                return State.FAILED;
            }
            return state;
        }

        if ("RUNNING".equalsIgnoreCase(status)) {
            if (state == State.SCHEDULED) {
                return State.RUNNING;
            }
            return state;
        }

        if ("SUSPENDED".equalsIgnoreCase(status)) {
            if (state == State.SCHEDULED) {
                return State.RUNNING;
            }
            return state;
        }

        if ("PREP".equalsIgnoreCase(status)) {
            if (state == State.SCHEDULED) {
                return State.RUNNING;
            }
            return state;
        }

        throw new RuntimeException("Unknown status of OozieWorkflowJob '" + status + "'");
    }

    public boolean synchronizeTaskState(final Project project, final Task task) {
        log.debug("Current task {} state is {}", task.getId(), task.getState());
        final String oozieWorkflowId = task.getOozieWorkflowId();

        log.debug("Oozie workflow id {}", oozieWorkflowId);

        if (Objects.isNull(oozieWorkflowId) || oozieWorkflowId.isEmpty()) {
            task.setSynch(true);
            taskTrackerService.update(project, task);
            log.debug("New task {} state is {}", task.getId(), task.getState());
            return true;
        }

        try {
            final OozieWorkflowJob job = oozieService.findWorkflowJob(oozieWorkflowId).orElse(null);
            if (Objects.isNull(job)) {
                task.setSynch(true);
                taskTrackerService.update(project, task);
                log.debug("New task {} state is {}", task.getId(), task.getState());
                return true;
            }

            log.debug("{}", job);

            final OozieCoordinatorJob coordinatorJob = oozieService.findCoordinatorJob(job).orElse(null);
            log.debug("{}", coordinatorJob);

            final State state = oozieWfJobStatusToTaskState(job, task);
            if (taskTrackerService.isTerminalState(state)) {
                task.setSynch(true);
                task.setState(state);

                if (Objects.nonNull(coordinatorJob)) {
                    task.setOozieCoordinatorId(coordinatorJob.getId());
                    task.setNextDate(coordinatorJob.getNextMatdTime());
                }

                taskTrackerService.update(project, task);
                log.debug("New task {} state is {}", task.getId(), task.getState());
                return true;
            }
        } catch (OozieServiceException e) {
            log.warn("Error while task synchronization", e);
        }

        return false;
    }
}
