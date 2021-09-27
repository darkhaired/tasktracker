package tasktracker.backend.service;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import tasktracker.backend.eventbus.TaskTrackerEventBus;
import tasktracker.backend.repository.*;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractServiceTest {
    @Mock
    protected ProjectRepository projectRepository;
    @Mock
    protected ProjectSettingsRepository projectSettingsRepository;
    @Mock
    protected TaskRepository taskRepository;
    @Mock
    protected TaskMetricRepository taskMetricRepository;
    @Mock
    protected TaskErrorRepository taskErrorRepository;
    @Mock
    protected TaskTypeRepository taskTypeRepository;
    @Mock
    protected TaskStatsRepository statsRepository;
    @Mock
    protected TaskTrackerEventBus eventBus;
    @Mock
    protected WarningRepository warningRepository;
    @Mock
    protected DataQualityRuleRepository ruleRepository;
    @Mock
    protected DataQualityConditionRepository conditionRepository;
}
