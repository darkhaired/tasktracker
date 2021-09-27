package tasktracker.backend.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;

import java.util.Date;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static tasktracker.backend.TestHelper.task;
import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateString;

@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@Import({MyTestConfiguration.class})
public class TaskRepositoryTest {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @Test
    public void findTasks() {
        Project project = new Project();
        project.setName("TestProject");
        project = projectRepository.save(project);

        taskRepository.save(task(project, "TestTask1", "SUCCEEDED", "2020-01-01T08:00:00.000+00:00", "2020-01-05T12:00:00.000+00:00"));
        taskRepository.save(task(project, "TestTask2", "SUCCEEDED", "2020-05-05T07:01:00.000+00:00", "2020-02-05T11:00:00.000+00:00"));

        Date from = getDateFromFormattedDateString("2020-01-01T07:00:00.000+00:00");
        Date to = getDateFromFormattedDateString("2020-02-05T07:00:00.000+00:00");
        List<Task> tasks = taskRepository.findTasks(1L, from, to);

        Assert.assertThat(tasks, hasSize(1));
        Assert.assertThat(tasks.get(0).getName(), equalTo("TestTask1"));
    }

}