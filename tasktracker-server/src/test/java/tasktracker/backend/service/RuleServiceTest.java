package tasktracker.backend.service;

import org.assertj.core.util.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tasktracker.backend.controller.model.dataqualityfrontend.FrontendDQRule;
import tasktracker.backend.dq.ExpressionsEvaluator;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static tasktracker.backend.TestHelper.dqRule;


public class RuleServiceTest extends AbstractServiceTest {
    private static Project project;
    @InjectMocks
    private RuleService ruleService;
    @Mock
    private TaskTrackerService taskTrackerService;
    @InjectMocks
    private ExpressionsEvaluator expressionsEvaluator;

    @BeforeClass
    public static void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("TestProject");
    }

    @Test
    public void findByProjectAndTaskName() {
        List<DataQualityRule> projectRules = Lists.newArrayList();
        projectRules.add(dqRule(1L, project, "stg.stg_table_1", "TestTask1", "caption_aab", Lists.emptyList()));
        projectRules.add(dqRule(2L, project, "stg.stg_table_2", "TestTask2", "caption_imei", Lists.emptyList()));

        when(ruleRepository.findByProject(project)).thenReturn(projectRules);

        List<DataQualityRule> filteredRules = ruleService.findByProjectAndTaskName(project, "TestTask1");

        assertThat(filteredRules, hasSize(1));
        assertThat(filteredRules, hasItem(allOf(hasProperty("tableName", equalTo("stg.stg_table_1")),
                hasProperty("caption", equalTo("caption_aab")))));
    }

    @Test
    public void collectionToFrontendFormat() {
        DataQualityRule rule = new DataQualityRule();
        rule.setId(1L);
        rule.setCaption("test_caption");
        rule.setTaskName("TestTask1");
        rule.setTableName("stg.stg_table_1");
        rule.setProject(project);

        DataQualityCondition condition = new DataQualityCondition();
        condition.setId(11L);
        condition.setColumnName("row_cnt");
        condition.setMetric(DataQualityCondition.Metric.count);
        condition.setExpression("confidence_interval_sigma('mean',1,10,true)");

        rule.setConditions(Lists.newArrayList(condition));

        FrontendDQRule frontendDQRule = ruleService.collectionToFrontendFormat(rule, expressionsEvaluator.getFunctions());

        assertThat(frontendDQRule, allOf(
                hasProperty("id", equalTo(1L)),
                hasProperty("taskName", equalTo("TestTask1")),
                hasProperty("tableName", equalTo("stg.stg_table_1"))
        ));
        assertThat(frontendDQRule.getConditions(), hasItem(allOf(
                hasProperty("id", equalTo(11L)),
                hasProperty("columnName", equalTo("row_cnt")),
                hasProperty("metric", equalTo("count")),
                hasProperty("function", equalTo("confidence_interval_sigma")))
        ));

        Map<String, String> args = frontendDQRule.getConditions().get(0).getArgs();
        assertThat(args.size(), equalTo(4));
        assertThat(args, allOf(
                hasEntry("center function", "'mean'"),
                hasEntry("k", "1"),
                hasEntry("stats number", "10"),
                hasEntry("delta", "true")
        ));
    }
}