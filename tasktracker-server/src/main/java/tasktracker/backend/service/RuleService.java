package tasktracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.controller.model.dataqualityfrontend.FrontendDQCondition;
import tasktracker.backend.controller.model.dataqualityfrontend.FrontendDQRule;
import tasktracker.backend.dq.DataQualityFunction;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;
import tasktracker.backend.repository.DataQualityConditionRepository;
import tasktracker.backend.repository.DataQualityRuleRepository;
import tasktracker.backend.repository.TaskStatsRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RuleService {
    private final DataQualityRuleRepository ruleRepository;
    private final DataQualityConditionRepository conditionRepository;
    private final TaskStatsRepository statsRepository;

    @Transactional
    public DataQualityRule save(final Project project, final DataQualityRule rule) {
        rule.setProject(project);
        rule.getConditions().forEach(condition ->
                condition.setDataQualityRule(rule)
        );

        return ruleRepository.save(rule);
    }

    @Transactional
    public void delete(final DataQualityRule rule) {
        ruleRepository.delete(rule);
    }

    public List<DataQualityCondition> findConditions(final DataQualityRule rule) {
        return conditionRepository.findByDataQualityRule(rule);
    }

    public List<DataQualityRule> findRules(final Project project) {
        return ruleRepository.findByProject(project);
    }

    public Optional<DataQualityRule> findRule(final Long id) {
        return ruleRepository.findById(id);
    }

    public List<DataQualityRule> findByProjectAndTaskName(final Project project, final String taskName) {
        List<DataQualityRule> projectRules = ruleRepository.findByProject(project);

        if (StringUtils.isNotBlank(taskName)) {
            return projectRules.stream()
                    .filter(rule -> rule.getTaskName().equalsIgnoreCase(taskName))
                    .collect(Collectors.toList());
        }
        return projectRules;
    }

    public FrontendDQRule collectionToFrontendFormat(final DataQualityRule rule, final List<DataQualityFunction> functions) {
        FrontendDQRule frontendDQRule = new FrontendDQRule();
        frontendDQRule.setId(rule.getId());
        frontendDQRule.setTaskName(rule.getTaskName());
        frontendDQRule.setCaption(rule.getCaption());
        frontendDQRule.setTableName(rule.getTableName());
        frontendDQRule.setConditions(rule.getConditions()
                .stream()
                .map(condition -> new FrontendDQCondition(condition, functions))
                .collect(Collectors.toList())
        );

        return frontendDQRule;
    }
}
