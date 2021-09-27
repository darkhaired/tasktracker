package tasktracker.backend.controller;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.controller.model.DataQualityConditionModel;
import tasktracker.backend.controller.model.DataQualityRuleModel;
import tasktracker.backend.dq.DataQualityRuleModelValidator;
import tasktracker.backend.dq.ExpressionsEvaluator;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;
import tasktracker.backend.service.RuleService;
import tasktracker.backend.service.TaskTrackerService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DataQualityController.class, secure = false)
@Import({MyTestConfiguration.class, ApiExceptionHandler.class})
public class DataQualityControllerTest extends AbstractMvcTest {
    @InjectMocks
    private DataQualityController controller;
    @MockBean
    private TaskTrackerService taskTrackerService;
    @MockBean
    private RuleService ruleService;
    @MockBean
    private DataQualityRuleModelValidator ruleValidator;
    @MockBean
    private ExpressionsEvaluator expressionsEvaluator;

    @Test
    public void createRule() throws Exception {
        Project project = new Project();
        project.setName("TestProject");
        project.setId(1L);

        DataQualityRuleModel ruleModel = new DataQualityRuleModel();
        ruleModel.setCaption("test_caption");
        ruleModel.setTableName("stg.stg_task");
        ruleModel.setTaskName("TestTask");

        List<DataQualityConditionModel> conditionModels = Lists.newArrayList();
        DataQualityConditionModel conditionModel = new DataQualityConditionModel();
        conditionModel.setColumnName("cnt_out_ind");
        conditionModel.setMetric("max");
        conditionModel.setExpression("is_above(5)");
        conditionModels.add(conditionModel);

        ruleModel.setConditions(conditionModels);

        DataQualityRule rule = new DataQualityRule();
        rule.setId(1L);
        ruleModel.setTableName("stg.stg_task");
        ruleModel.setTaskName("TestTask");

        when(taskTrackerService.findProjectById(eq(1L))).thenReturn(Optional.of(project));
        doNothing().when(ruleValidator).validate(any(), any());
        when(ruleService.save(any(), any())).thenReturn(rule);

        mvc().perform(post("/api/v2/projects/" + project.getId() + "/dataquality/rules")
                .contentType("application/json")
                .content(toJson(ruleModel)))
                .andExpect(status().isOk());
    }




}