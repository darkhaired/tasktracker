package tasktracker.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tasktracker.backend.eventbus.TaskTrackerEventConsumer;


@TestConfiguration
public class MyTestConfiguration {
    @Bean
    public TaskTrackerEventConsumer consumer() {
        TaskTrackerEventConsumer consumer = (message) -> {
        };
        return consumer;
    }
}
