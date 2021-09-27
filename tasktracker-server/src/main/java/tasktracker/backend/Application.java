package tasktracker.backend;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tasktracker.backend.eventbus.DefaultEventBus;
import tasktracker.backend.eventbus.TaskTrackerEventBus;
import tasktracker.backend.eventbus.TaskTrackerEventConsumer;

import java.util.Collection;

@SpringBootApplication
@EnableScheduling
public class Application {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	public Application() {
		logger.info("TaskTracker server started");
	}

	@Bean
	public TaskTrackerEventBus eventBus(final Collection<TaskTrackerEventConsumer> consumers) {
		final TaskTrackerEventBus eventBus = new DefaultEventBus();

		consumers.forEach(eventBus::register);

		return eventBus;
	}

	@Bean
	public ThreadPoolTaskExecutor commonThreadPool() {
		final ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("common-pool-%d").build());

		return pool;
	}
}
