package tasktracker.backend.service;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TaskStateUpdaterScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskTrackerService service;
    private final TaskStateUpdater taskStateUpdater;

//     one per five minutes
//    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void autoUpdateTaskStateStatus() {
        logger.info("Auto update task status");

        taskStateUpdater.update(service);

        logger.info("Auto update task status completed");
    }
}
