package tasktracker.backend.controller;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.controller.mappers.TaskStatsMapper;
import tasktracker.backend.controller.model.TasksReportResponse;
import tasktracker.backend.model.Project;
import tasktracker.backend.service.TaskTrackerService;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class, secure = false)
@Import({MyTestConfiguration.class, TaskStatsMapper.class, WarningMapper.class})
public class TaskControllerTest extends AbstractMvcTest {

    private static Project project;
    @MockBean
    private TaskTrackerService service;

    @BeforeClass
    public static void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("test");
    }

    @Test
    public void tasksReportOk() throws Exception {
        when(service.findProjectById(any())).thenReturn(Optional.of(project));
        mvc().perform(get("/api/v2/projects/" + project.getId() + "/tasks/report")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-05")
                .param("dateType", "NOMINAL_DATE")
                .param("fields", "statistics,errors,metrics"))
                .andExpect(status().isOk());
    }

    @Test
    public void tasksReportMissingStartEndDate() throws Exception {
        when(service.findProjectById(any())).thenReturn(Optional.of(project));
        mvc().perform(get("/api/v2/projects/" + project.getId() + "/tasks/report")
                .param("dateType", "NOMINAL_DATE")
                .param("fields", "statistics,errors,metrics"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void tasksReportEmptyResult() throws Exception {
        when(service.findProjectById(any())).thenReturn(Optional.of(project));
        final String json = mvc().perform(get("/api/v2/projects/" + project.getId() + "/tasks/report")
                .param("dateType", "NOMINAL_DATE")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-05")
                .param("lastTaskOnly", "true")
                .param("fields", "statistics,errors,metrics"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final TasksReportResponse response = fromJson(json, TasksReportResponse.class);

        System.out.println("response = " + response);
        assertNotNull(response);
        assertNotNull(response.getTasks());
        assertEquals(0, response.getTasks().size());
    }
}