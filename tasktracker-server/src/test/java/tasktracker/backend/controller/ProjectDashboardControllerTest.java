package tasktracker.backend.controller;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.controller.groupers.Frequency;
import tasktracker.backend.controller.model.dashboard.DistributionChart;
import tasktracker.backend.model.Project;
import tasktracker.backend.service.TaskTrackerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectDashboardController.class, secure = false)
@Import({MyTestConfiguration.class})
public class ProjectDashboardControllerTest extends AbstractMvcTest {
    @InjectMocks
    private ProjectDashboardController controller;
    @MockBean
    private TaskTrackerService taskTrackerService;

    private Project project;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("test");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    public void topFailedTasks() throws Exception {
        LinkedHashMap<String, Integer> topTasks = Maps.newLinkedHashMap();
        topTasks.put("TestTask1", 25);
        topTasks.put("TestTask2", 8);

        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        when(taskTrackerService.findTopFailedTasksStats(eq(project), any(Date.class), any(Date.class), eq(10)))
                .thenReturn(topTasks);

        mvc().perform(get("/api/v2/projects/" + project.getId() + "/dashboard/topFailedTasks")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-31")
                .param("top", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.top_elements", hasSize(2)))
                .andExpect(jsonPath("$.top_elements[0].counter", equalTo(25)))
                .andExpect(jsonPath("$.top_elements[0].name", equalTo("TestTask1")))
                .andExpect(jsonPath("$.top_elements[1].counter", equalTo(8)))
                .andExpect(jsonPath("$.top_elements[1].name", equalTo("TestTask2")));
    }

    @Test
    public void testTopIsMissing() throws Exception {
        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        final String json = mockMvc.perform(get("/api/v2/projects/" + project.getId() + "/dashboard/topFailedTasks")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-31"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        final ErrorResponse response = fromJson(json, ErrorResponse.class);

        assertNotNull(response);
        assertThat(response.getErrors(), hasSize(1));
        assertThat(response.getErrors().get(0), allOf(
                hasProperty("message", equalTo("Missing param 'top'"))
        ));
    }

    @Test
    public void testTopIsNull() throws Exception {
        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        final String json = mockMvc.perform(get("/api/v2/projects/" + project.getId() + "/dashboard/topFailedTasks")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-31")
                .param("top", "null"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        final ErrorResponse response = fromJson(json, ErrorResponse.class);

        assertNotNull(response);
        assertThat(response.getErrors(), hasSize(1));
        assertThat(response.getErrors().get(0), allOf(
                hasProperty("message", equalTo("Bad param 'null'"))
        ));
    }

    @Test
    public void topLongTasks() throws Exception {
        LinkedHashMap<String, Integer> topLongTasks = Maps.newLinkedHashMap();
        topLongTasks.put("TestTask1", 1500);
        topLongTasks.put("TestTask2", 800);

        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        when(taskTrackerService.findTopLongTasksStats(eq(project), any(Date.class), any(Date.class), eq(10)))
                .thenReturn(topLongTasks);

        mvc().perform(get("/api/v2/projects/" + project.getId() + "/dashboard/topLongTasks")
                .param("startDate", "2020-01-01")
                .param("endDate", "2020-01-31")
                .param("top", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.top_elements", hasSize(2)))
                .andExpect(jsonPath("$.top_elements[0].counter", equalTo(1500)))
                .andExpect(jsonPath("$.top_elements[0].name", equalTo("TestTask1")))
                .andExpect(jsonPath("$.top_elements[1].counter", equalTo(800)))
                .andExpect(jsonPath("$.top_elements[1].name", equalTo("TestTask2")));
    }

    @Test
    public void tasksDistribution() throws Exception {
        Date d1 = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date d2 = DateTimePatterns.getDateFromFormattedDateString("2020-05-08");
        LinkedHashMap<Date, Integer> tasksDistribution = Maps.newLinkedHashMap();
        tasksDistribution.put(d1, 15);
        tasksDistribution.put(d2, 18);

        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        when(taskTrackerService.getTasksDistribution(eq(project), any(Date.class), any(Date.class), eq(Frequency.WEEK)))
                .thenReturn(tasksDistribution);

        final String json = mvc().perform(get("/api/v2/projects/" + project.getId() + "/dashboard/tasksDistribution")
                .param("startDate", "2020-05-01")
                .param("endDate", "2020-05-10")
                .param("timePeriod", "WEEK"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final DistributionChart chart = fromJson(json, DistributionChart.class);

        assertNotNull(chart);

        assertThat(chart.getStartDate(), equalTo("2020-05-01"));
        assertThat(chart.getEndDate(), equalTo("2020-05-10"));
        assertThat(chart.getTimePeriod(), equalTo("WEEK"));
        assertThat(chart.getData(), hasSize(2));

        String timeOffset = new SimpleDateFormat("Z").format(new Date());

        assertThat(chart.getData(), hasItem(allOf(
                hasProperty("datetime", equalTo(String.format("2020-05-01T00:00:00.000%s", timeOffset))),
                hasProperty("counter", equalTo(15))
        )));
        assertThat(chart.getData(), hasItem(allOf(
                hasProperty("datetime", equalTo(String.format("2020-05-08T00:00:00.000%s", timeOffset))),
                hasProperty("counter", equalTo(18))
        )));
    }
}