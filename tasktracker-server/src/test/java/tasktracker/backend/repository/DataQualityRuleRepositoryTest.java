package tasktracker.backend.repository;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;


@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@Import({MyTestConfiguration.class})
public class DataQualityRuleRepositoryTest {

    @Autowired
    private DataQualityRuleRepository dataQualityRuleRepository;
    @Autowired
    private ProjectRepository projectRepository;

    //    Убеждаемся, что
//      1) происходит сохранение правила и ограничений
//      2) изменение/удаление ограничений сохраняется
    @Test
    public void save() {
        Project project = new Project();
        project.setName("Test");
        project = projectRepository.save(project);

        DataQualityRule rule = new DataQualityRule();
        rule.setProject(project);
        rule.setCaption("test_caption");
        rule.setTaskName("TestTask");
        rule.setTableName("stg.stg_task");

        List<DataQualityCondition> conditions = Lists.newArrayList();

        DataQualityCondition condition1 = new DataQualityCondition();
        condition1.setColumnName("agg_val");
        condition1.setMetric(DataQualityCondition.Metric.max);
        condition1.setExpression("is_above(500)");
        conditions.add(condition1);

        DataQualityCondition condition2 = new DataQualityCondition();
        condition2.setColumnName("agg_val");
        condition2.setMetric(DataQualityCondition.Metric.std_dev);
        condition2.setExpression("is_above(800)");
        conditions.add(condition2);

        rule.setConditions(conditions);
        rule = dataQualityRuleRepository.save(rule);

        Assert.assertThat(rule.getConditions(), hasSize(2));

        DataQualityCondition firstCondition = rule.getConditions().get(0);
        firstCondition.setMetric(DataQualityCondition.Metric.total_count);
        firstCondition.setExpression("is_below(777)");

        rule.getConditions().remove(1);

        DataQualityRule updatedRule = dataQualityRuleRepository.save(rule);
        Assert.assertThat(updatedRule.getConditions(), hasSize(1));
        DataQualityCondition updatedCondition = updatedRule.getConditions().get(0);

        Assert.assertThat(updatedCondition.getExpression(), equalTo("is_below(777)"));
        Assert.assertThat(updatedCondition.getMetric(), equalTo(DataQualityCondition.Metric.total_count));
    }
}