package tasktracker.backend.dq;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tasktracker.backend.model.*;
import tasktracker.backend.repository.WarningRepository;
import tasktracker.backend.service.RuleService;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class DataQualityChecker {

    private final WarningRepository warningRepository;
    private final RuleService ruleService;
    private final ExpressionsEvaluator expressionsEvaluator;

    public void applyBasicChecks(final Task task, final List<TaskStats> statistics) {
        if (task.getAnalyzed()) {
            return;
        }

        if (statistics.isEmpty()) {
            return;
        }

        final List<Warning> warnings = Lists.newArrayList();

        for (TaskStats stats : statistics) {
            Optional<Warning> warning = checkTotalCount(task, stats);
            if (warning.isPresent()) {
                warnings.add(warning.get());
                break;
            }
        }

        statistics.forEach(stats -> {
            checkNullCount(task, stats).ifPresent(warnings::add);
        });

        warnings.forEach(warningRepository::save);
    }

    public Optional<Warning> checkTotalCount(final Task task, final TaskStats stats) {
        final Long totalCount = stats.getTotalCount();
        if (Objects.isNull(totalCount) || totalCount == 0) {
            final Warning warning = new Warning();
            warning.setTask(task);
            warning.setCreatedTime(new Date());
            warning.setMessage("Количество строк равно 0");
            return Optional.of(warning);
        }

        return Optional.empty();
    }

    public Optional<Warning> checkNullCount(final Task task, final TaskStats stats) {
        final Long totalCount = stats.getTotalCount();
        if (Objects.isNull(totalCount) || totalCount == 0) {
            return Optional.empty();
        }

        if (stats.getColumnType() == TaskStats.ColumnType.OBJECT) {
            return Optional.empty();
        }

        final Long count = stats.getCount();
        if (Objects.isNull(count) || count == 0) {
            final Warning warning = new Warning();
            warning.setTask(task);
            warning.setCreatedTime(new Date());
            warning.setMessage(
                    "Для колонки [" + stats.getColumn() + "]" + " все строки имееют значения NULL"
            );
            return Optional.of(warning);
        }

        return Optional.empty();
    }

    public void applyDataQualityChecks(final Project project, final Task task, final List<TaskStats> statistics) {
        if (task.getAnalyzed()) {
            return;
        }

        if (statistics.isEmpty()) {
            return;
        }

        final List<Warning> warnings = Lists.newArrayList();
        final List<DataQualityRule> rules = ruleService.findRules(project);


        statistics.forEach(taskStats -> {

            rules.forEach(rule -> {
                String tableName = rule.getTableName();
                rule.getConditions().forEach(condition -> {
                    if (taskStats.getColumn().equals(tableName + "." + condition.getColumnName())) {
                        try {
                            log.info(String.format("Start checking condition [%s] of rule [%s] for stats [%s]",
                                    condition,
                                    rule,
                                    taskStats));
                            ExpressionsEvaluator.ExpressionEvaluatorResponse result = expressionsEvaluator.isFullfilled(project, task, taskStats, condition);
                            if (!result.isFullfilled()) {
                                String warningMessage = String.format(
                                        "Condition [%d] of rule [%d] %s is not fullfilled. %s",
                                        condition.getId(),
                                        rule.getId(),
                                        condition.getExpression(),
                                        result.getMessage());

                                Warning warning = new Warning();
                                warning.setTask(task);
                                warning.setMessage(warningMessage);
                                warnings.add(warning);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            });
        });
        warnings.forEach(warningRepository::save);
    }

}
