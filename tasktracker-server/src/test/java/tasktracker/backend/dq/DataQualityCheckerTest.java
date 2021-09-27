package tasktracker.backend.dq;

import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tasktracker.backend.MyTestConfiguration;
import tasktracker.backend.model.*;
import tasktracker.backend.repository.*;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.sqrt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateString;

@RunWith(SpringJUnit4ClassRunner.class)
@Import({MyTestConfiguration.class})
@SpringBootTest
public class DataQualityCheckerTest {
    @Autowired
    private DataQualityChecker dataQualityChecker;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private TaskStatsRepository statsRepository;
    @Autowired
    private DataQualityRuleRepository ruleRepository;

    @Test
    public void isBelowOrAbove() {
        Project project = new Project();
        project.setName("TestProject");
        project = projectRepository.save(project);

        Task task = new Task();
        task.setName("TestTask");
        task.setProject(project);
        task = taskRepository.save(task);

        List<TaskStats> stats = Lists.newArrayList();
        TaskStats stat = new TaskStats();
        stat.setTask(task);
        stat.setColumn("stg.test_task.ind_1");
        stat.setMax(98.0);
        stat.setMin(32.0);
        stat = statsRepository.save(stat);

        stats.add(stat);

        DataQualityRule rule = new DataQualityRule();
        rule.setTableName("stg.test_task");
        rule.setProject(project);

        List<DataQualityCondition> conditions = Lists.newArrayList();
        DataQualityCondition condition = new DataQualityCondition();
        condition.setDataQualityRule(rule);
        condition.setColumnName("ind_1");
        condition.setMetric(DataQualityCondition.Metric.max);
        condition.setExpression("is_below(90)");
        conditions.add(condition);

        rule.setConditions(conditions);
        rule = ruleRepository.save(rule);

        dataQualityChecker.applyDataQualityChecks(project, task, stats);

        List<Warning> warnings = warningRepository.findByTask(task);

        assertThat(warnings, hasSize(1));
        assertThat(warnings, hasItem(
                hasProperty("message",
                        equalTo(String.format("Condition [%d] of rule [%d] %s is not fullfilled. TaskStats [%d], metric max = %f is not below %s",
                                rule.getConditions().get(0).getId(),
                                rule.getId(),
                                rule.getConditions().get(0).getExpression(),
                                stat.getId(),
                                stat.getMetric(DataQualityCondition.Metric.max),
                                "90,000000")))
        ));

    }

    @Test
    public void confidentialIntervalNoWarning() {
        String statsColumnName = "stg.stg_task";

        Project project = new Project();
        project.setName("TestProject");
        project = projectRepository.save(project);

        Task task1 = new Task();
        task1.setName("TestTask");
        task1.setState(Task.State.SUCCEEDED);
        task1.setProject(project);
        task1.setNominalDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1.setStartDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1 = taskRepository.save(task1);

        TaskStats taskStats1 = new TaskStats();
        taskStats1.setTask(task1);
        taskStats1.setColumn(statsColumnName);
        taskStats1.setCount(10L);
        taskStats1 = statsRepository.save(taskStats1);


        Task task2 = new Task();
        task2.setName("TestTask");
        task2.setState(Task.State.SUCCEEDED);
        task2.setProject(project);
        task2.setNominalDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2.setStartDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2 = taskRepository.save(task2);

        TaskStats taskStats2 = new TaskStats();
        taskStats2.setTask(task2);
        taskStats2.setColumn(statsColumnName);
        taskStats2.setCount(40L);
        taskStats2 = statsRepository.save(taskStats2);

        Task task3 = new Task();
        task3.setName("TestTask");
        task3.setState(Task.State.SUCCEEDED);
        task3.setProject(project);
        task3.setNominalDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3.setStartDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3 = taskRepository.save(task3);

        TaskStats taskStats3 = new TaskStats();
        taskStats3.setTask(task3);
        taskStats3.setColumn(statsColumnName);
        taskStats3.setCount(60L);
        taskStats3 = statsRepository.save(taskStats3);

        Task task4 = new Task();
        task4.setName("TestTask");
        task4.setState(Task.State.SUCCEEDED);
        task4.setProject(project);
        task4.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4.setStartDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4 = taskRepository.save(task4);

        TaskStats taskStats4 = new TaskStats();
        taskStats4.setTask(task4);
        taskStats4.setColumn(statsColumnName);
        taskStats4.setCount(10L);
        taskStats4 = statsRepository.save(taskStats4);

        Task task42 = new Task();
        task42.setName("TestTask");
        task42.setState(Task.State.SUCCEEDED);
        task42.setProject(project);
        task42.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task42.setStartDate(getDateFromFormattedDateString("2020-07-04T12:00:00.000+0000"));
        task42 = taskRepository.save(task42);

        TaskStats taskStats42 = new TaskStats();
        taskStats42.setTask(task42);
        taskStats42.setColumn(statsColumnName);
        taskStats42.setCount(50L);
        taskStats42 = statsRepository.save(taskStats42);

        Task task5 = new Task();
        task5.setName("TestTask");
        task5.setState(Task.State.SUCCEEDED);
        task5.setProject(project);
        task5.setNominalDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5.setStartDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5 = taskRepository.save(task5);

        TaskStats taskStats5 = new TaskStats();
        taskStats5.setTask(task5);
        taskStats5.setColumn(statsColumnName);
        taskStats5.setCount(57L);
        taskStats5 = statsRepository.save(taskStats5);

//      Create rule
        DataQualityRule rule = new DataQualityRule();
        rule.setTableName("stg.stg_task");
        rule.setProject(project);

        List<DataQualityCondition> conditions = Lists.newArrayList();

        DataQualityCondition condition = new DataQualityCondition();
        condition.setDataQualityRule(rule);
        condition.setColumnName("cnt");
        condition.setMetric(DataQualityCondition.Metric.count);
        condition.setExpression("confidence_interval_sigma('mean', 1, 3, false)");
        conditions.add(condition);

        rule.setConditions(conditions);
        rule = ruleRepository.save(rule);

        dataQualityChecker.applyDataQualityChecks(project, task5, Arrays.asList(taskStats5));

        List<Warning> warnings = warningRepository.findByTask(task5);

        assertThat(warnings, is(empty()));
    }

    @Test
    public void confidentialIntervalWithoutDeltaWarning() {
        String statsColumnName = "stg.stg_task.cnt";

        Project project = new Project();
        project.setName("TestProject");
        project = projectRepository.save(project);

        Task task1 = new Task();
        task1.setName("TestTask");
        task1.setState(Task.State.SUCCEEDED);
        task1.setProject(project);
        task1.setNominalDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1.setStartDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1 = taskRepository.save(task1);

        TaskStats taskStats1 = new TaskStats();
        taskStats1.setTask(task1);
        taskStats1.setColumn(statsColumnName);
        taskStats1.setCount(10L);
        taskStats1 = statsRepository.save(taskStats1);


        Task task2 = new Task();
        task2.setName("TestTask");
        task2.setState(Task.State.SUCCEEDED);
        task2.setProject(project);
        task2.setNominalDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2.setStartDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2 = taskRepository.save(task2);

        TaskStats taskStats2 = new TaskStats();
        taskStats2.setTask(task2);
        taskStats2.setColumn(statsColumnName);
        taskStats2.setCount(40L);
        taskStats2 = statsRepository.save(taskStats2);

        Task task3 = new Task();
        task3.setName("TestTask");
        task3.setState(Task.State.SUCCEEDED);
        task3.setProject(project);
        task3.setNominalDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3.setStartDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3 = taskRepository.save(task3);

        TaskStats taskStats3 = new TaskStats();
        taskStats3.setTask(task3);
        taskStats3.setColumn(statsColumnName);
        taskStats3.setCount(60L);
        taskStats3 = statsRepository.save(taskStats3);

        Task task4 = new Task();
        task4.setName("TestTask");
        task4.setState(Task.State.SUCCEEDED);
        task4.setProject(project);
        task4.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4.setStartDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4 = taskRepository.save(task4);

        TaskStats taskStats4 = new TaskStats();
        taskStats4.setTask(task4);
        taskStats4.setColumn(statsColumnName);
        taskStats4.setCount(10L);
        taskStats4 = statsRepository.save(taskStats4);

        Task task42 = new Task();
        task42.setName("TestTask");
        task42.setState(Task.State.SUCCEEDED);
        task42.setProject(project);
        task42.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task42.setStartDate(getDateFromFormattedDateString("2020-07-04T12:00:00.000+0000"));
        task42 = taskRepository.save(task42);

        TaskStats taskStats42 = new TaskStats();
        taskStats42.setTask(task42);
        taskStats42.setColumn(statsColumnName);
        taskStats42.setCount(50L);
        taskStats42 = statsRepository.save(taskStats42);

        Task task5 = new Task();
        task5.setName("TestTask");
        task5.setState(Task.State.SUCCEEDED);
        task5.setProject(project);
        task5.setNominalDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5.setStartDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5 = taskRepository.save(task5);

        TaskStats taskStats5 = new TaskStats();
        taskStats5.setTask(task5);
        taskStats5.setColumn(statsColumnName);
        taskStats5.setCount(59L);
        taskStats5 = statsRepository.save(taskStats5);

//      Create rule
        DataQualityRule rule = new DataQualityRule();
        rule.setTableName("stg.stg_task");
        rule.setProject(project);

        List<DataQualityCondition> conditions = Lists.newArrayList();

        DataQualityCondition condition = new DataQualityCondition();
        condition.setDataQualityRule(rule);
        condition.setColumnName("cnt");
        condition.setMetric(DataQualityCondition.Metric.count);
        condition.setExpression("confidence_interval_sigma('mean', 1, 3, false)");
        conditions.add(condition);

        rule.setConditions(conditions);
        rule = ruleRepository.save(rule);

        dataQualityChecker.applyDataQualityChecks(project, task5, Arrays.asList(taskStats5));

        List<Warning> warnings = warningRepository.findByTask(task5);

        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        descriptiveStatistics.addValue(50.0);
        descriptiveStatistics.addValue(60.0);
        descriptiveStatistics.addValue(40.0);


        Double centralFuncValue = descriptiveStatistics.getMean();
        Double stdDev = sqrt(descriptiveStatistics.getPopulationVariance());
        Double from = centralFuncValue - 1 * stdDev;
        Double to = centralFuncValue + 1 * stdDev;

        System.out.println("warnings = " + warnings);
        assertThat(warnings, hasSize(1));
        assertThat(warnings, hasItem(
                hasProperty("message",
                        equalTo(String.format("Condition [%d] of rule [%d] %s is not fullfilled. TaskStats [%d], %s = %f, interval = [%f ; %f]",
                                rule.getConditions().get(0).getId(),
                                rule.getId(),
                                rule.getConditions().get(0).getExpression(),
                                taskStats5.getId(),
                                taskStats5.getColumn() + "." + condition.getMetric().name(),
                                taskStats5.getMetric(DataQualityCondition.Metric.count),
                                from,
                                to)
                        ))));
    }

    @Test
    public void confidentialIntervalWithDeltaWarning() {
        String statsColumnName = "stg.stg_task.cnt";

        Project project = new Project();
        project.setName("TestProject");
        project = projectRepository.save(project);

        Task task1 = new Task();
        task1.setName("TestTask");
        task1.setState(Task.State.SUCCEEDED);
        task1.setProject(project);
        task1.setNominalDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1.setStartDate(getDateFromFormattedDateString("2020-07-01T08:00:00.000+0000"));
        task1 = taskRepository.save(task1);

        TaskStats taskStats1 = new TaskStats();
        taskStats1.setTask(task1);
        taskStats1.setColumn(statsColumnName);
        taskStats1.setCount(10L);
        taskStats1 = statsRepository.save(taskStats1);


        Task task2 = new Task();
        task2.setName("TestTask");
        task2.setState(Task.State.SUCCEEDED);
        task2.setProject(project);
        task2.setNominalDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2.setStartDate(getDateFromFormattedDateString("2020-07-02T08:00:00.000+0000"));
        task2 = taskRepository.save(task2);

        TaskStats taskStats2 = new TaskStats();
        taskStats2.setTask(task2);
        taskStats2.setColumn(statsColumnName);
        taskStats2.setCount(15L);
        taskStats2 = statsRepository.save(taskStats2);

        Task task3 = new Task();
        task3.setName("TestTask");
        task3.setState(Task.State.SUCCEEDED);
        task3.setProject(project);
        task3.setNominalDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3.setStartDate(getDateFromFormattedDateString("2020-07-03T08:00:00.000+0000"));
        task3 = taskRepository.save(task3);

        TaskStats taskStats3 = new TaskStats();
        taskStats3.setTask(task3);
        taskStats3.setColumn(statsColumnName);
        taskStats3.setCount(30L);
        taskStats3 = statsRepository.save(taskStats3);

        Task task4 = new Task();
        task4.setName("TestTask");
        task4.setState(Task.State.SUCCEEDED);
        task4.setProject(project);
        task4.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4.setStartDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task4 = taskRepository.save(task4);

        TaskStats taskStats4 = new TaskStats();
        taskStats4.setTask(task4);
        taskStats4.setColumn(statsColumnName);
        taskStats4.setCount(5L);
        taskStats4 = statsRepository.save(taskStats4);

        Task task42 = new Task();
        task42.setName("TestTask");
        task42.setState(Task.State.SUCCEEDED);
        task42.setProject(project);
        task42.setNominalDate(getDateFromFormattedDateString("2020-07-04T08:00:00.000+0000"));
        task42.setStartDate(getDateFromFormattedDateString("2020-07-04T12:00:00.000+0000"));
        task42 = taskRepository.save(task42);

        TaskStats taskStats42 = new TaskStats();
        taskStats42.setTask(task42);
        taskStats42.setColumn(statsColumnName);
        taskStats42.setCount(40L);
        taskStats42 = statsRepository.save(taskStats42);

        Task task5 = new Task();
        task5.setName("TestTask");
        task5.setState(Task.State.SUCCEEDED);
        task5.setProject(project);
        task5.setNominalDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5.setStartDate(getDateFromFormattedDateString("2020-07-05T08:00:00.000+0000"));
        task5 = taskRepository.save(task5);

        TaskStats taskStats5 = new TaskStats();
        taskStats5.setTask(task5);
        taskStats5.setColumn(statsColumnName);
        taskStats5.setCount(100L);
        taskStats5 = statsRepository.save(taskStats5);

//      Create rule
        DataQualityRule rule = new DataQualityRule();
        rule.setTableName("stg.stg_task");
        rule.setProject(project);

        List<DataQualityCondition> conditions = Lists.newArrayList();

        DataQualityCondition condition = new DataQualityCondition();
        condition.setDataQualityRule(rule);
        condition.setColumnName("cnt");
        condition.setMetric(DataQualityCondition.Metric.count);
        condition.setExpression("confidence_interval_sigma('mean', 1, 3, true)");
        conditions.add(condition);

        rule.setConditions(conditions);
        rule = ruleRepository.save(rule);

        dataQualityChecker.applyDataQualityChecks(project, task5, Arrays.asList(taskStats5));

        List<Warning> warnings = warningRepository.findByTask(task5);

        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        descriptiveStatistics.addValue(5.0);
        descriptiveStatistics.addValue(15.0);
        descriptiveStatistics.addValue(10.0);

        Double centralFuncValue = descriptiveStatistics.getPercentile(50);
        Double stdDev = sqrt(descriptiveStatistics.getPopulationVariance());
        Double from = centralFuncValue - 1 * stdDev;
        Double to = centralFuncValue + 1 * stdDev;

        System.out.println("warnings = " + warnings);
        assertThat(warnings, hasSize(1));
        assertThat(warnings, hasItem(
                hasProperty("message",
                        equalTo(String.format("Condition [%d] of rule [%d] %s is not fullfilled. TaskStats [%d], %s = %f, delta = %f, interval = [%f ; %f]",
                                rule.getConditions().get(0).getId(),
                                rule.getId(),
                                rule.getConditions().get(0).getExpression(),
                                taskStats5.getId(),
                                taskStats5.getColumn() + "." + condition.getMetric().name(),
                                taskStats5.getMetric(DataQualityCondition.Metric.count),
                                taskStats5.getMetric(DataQualityCondition.Metric.count) - taskStats42.getMetric(DataQualityCondition.Metric.count),
                                from,
                                to)
                        ))));
    }
}