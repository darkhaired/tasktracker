package tasktracker.backend.dq;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.controller.model.DataQualityConditionModel;
import tasktracker.backend.controller.model.DataQualityRuleModel;
import tasktracker.backend.model.Project;

import static org.hamcrest.Matchers.equalTo;


@RunWith(SpringJUnit4ClassRunner.class)
@Import({MyTestConfiguration.class})
@SpringBootTest
public class DataQualityRuleValidatorTest {
    private static Project project;
    @Autowired
    private ExpressionsEvaluator expressionsEvaluator;
    @Autowired
    private DataQualityConditionModelValidator conditionModelValidator;
    @Autowired
    private DataQualityRuleModelValidator ruleModelValidator;

    @BeforeClass
    public static void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("TestProject");
    }

    @Test
    public void validateInvalidCondition() {
        DataQualityRuleModel ruleModel = new DataQualityRuleModel();
        ruleModel.setId(1L);
        ruleModel.setCaption("test_caption");
        ruleModel.setTaskName("Task");
        ruleModel.setTableName("stg.stg_task");

        DataQualityConditionModel conditionModel = new DataQualityConditionModel();
        conditionModel.setId(11L);
        conditionModel.setColumnName("ctn");
        conditionModel.setMetric("count");
        conditionModel.setExpression("confidence_interval_sigma('meann',1,10,true)");

        ruleModel.setConditions(Lists.newArrayList(conditionModel));

        Errors errors = new BeanPropertyBindingResult(ruleModel, "ruleModel");

        ruleModelValidator.validate(ruleModel, errors);
        Assert.assertThat(errors.getAllErrors().get(0).getCode(), equalTo("1 argument of function confidence_interval_sigma can only take these values ['mean', 'median']"));
    }
}