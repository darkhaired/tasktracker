package tasktracker.backend.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.math.Quantiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.controller.DateTimePatterns;
import tasktracker.backend.controller.groupers.DateTaskGrouper;
import tasktracker.backend.controller.groupers.Frequency;
import tasktracker.backend.controller.groupers.NameDateTaskGrouper;
import tasktracker.backend.controller.mappers.TaskStatsMapper;
import tasktracker.backend.controller.model.*;
import tasktracker.backend.controller.model.dashboard.KeyValueModel;
import tasktracker.backend.controller.model.dashboard.TopChart;
import tasktracker.backend.eventbus.TaskTrackerEvent;
import tasktracker.backend.eventbus.TaskTrackerEventBus;
import tasktracker.backend.model.*;
import tasktracker.backend.repository.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.Entry.comparingByValue;
import static tasktracker.backend.controller.DateTimePatterns.*;
import static tasktracker.backend.model.Task.DateType;
import static tasktracker.backend.model.Task.State;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskTrackerService {
    private final ProjectRepository projectRepository;
    private final ProjectSettingsRepository projectSettingsRepository;
    private final TaskRepository taskRepository;
    private final TaskMetricRepository taskMetricRepository;
    private final TaskErrorRepository taskErrorRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final TaskStatsRepository statsRepository;
    private final TaskTrackerEventBus eventBus;
    private final WarningRepository warningRepository;
    private final DataQualityRuleRepository ruleRepository;

    public static String normalizeTaskName(final String name) {
        Preconditions.checkNotNull(name);
        return name.trim().toLowerCase();
    }

    public static <T> T measureTotalTime(final Supplier<T> supplier, final String label) {
        final long start = System.currentTimeMillis();
        try {
            return supplier.get();
        } finally {
            System.out.println(label + ". Total time: " + (System.currentTimeMillis() - start));
        }
    }

    public Optional<Project> findProjectById(final Long id) {
        if (Objects.isNull(id)) {
            return Optional.empty();
        }
        return projectRepository.findById(id);
    }

    public Optional<Project> findProjectByName(final String name) {
        if (Objects.isNull(name)) {
            return Optional.empty();
        }

        return projectRepository.findByNameIgnoreCase(name);
    }

    public Project save(final Project project) {
        project.setCreatedAt(new Date());
        return projectRepository.save(project);
    }

    public Optional<Task> findTaskById(final Long id) {
        return taskRepository.findById(id);
    }

    public List<Project> findProjects() {
        return projectRepository.findProjects();
    }

    private List<Task> findTasksByProjectId(final Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public List<TaskStats> findDistinctTaskStatsByProject(final Project project, final String taskName) {
        return taskRepository.findDistinctLatestSucceededTasksByProject(project.getId())
                .stream()
                .filter(task -> {
                    if (Objects.nonNull(taskName)) {
                        return task.getName().equalsIgnoreCase(StringUtils.trim(taskName));
                    }
                    return true;
                })
                .flatMap(task -> findStatistics(task).stream())
                .collect(Collectors.toList());
    }

    public List<TableModel> tableToColumn(final Project project, final String taskName) {
        List<TaskStats> taskStats = findDistinctTaskStatsByProject(project, taskName);
        Map<String, List<ColumnModel>> tableToColumns = Maps.newHashMap();
        Map<String, String> tableToTaskNames = Maps.newHashMap();
        taskStats.forEach(stats -> {
            String[] t = stats.getColumn().split("\\.");
            if (t.length == 3) {
                String tableName = t[0] + "." + t[1];
                String columnName = t[2];
                tableToColumns.computeIfAbsent(tableName, (key) -> Lists.newArrayList())
                        .add(new ColumnModel(columnName, stats.getColumnType()));
                tableToTaskNames.put(tableName, stats.getTask().getName());
            }
        });

        List<TableModel> tableModels = Lists.newArrayList();
        tableToColumns.forEach((tableName, columnsList) -> {
            TableModel tableModel = new TableModel();
            tableModel.setTableName(tableName);
            tableModel.setColumnModels(columnsList);
            tableModel.setTaskName(tableToTaskNames.get(tableName));
            tableModels.add(tableModel);
        });

        return tableModels;
    }

    @Transactional(readOnly = true)
    public Optional<TaskStats> findTopTaskStatsByColumn(final String column) {
        return statsRepository.findTopByColumnOrderByIdDesc(column);
    }

    @Transactional(readOnly = true)
    public List<String> findAllTaskNames(final Long projectId) {
        return taskRepository.getTaskNames(projectId);
    }

    public Set<String> findTaskNamesByFilter(final Project project, final Set<String> filter) {
        final Set<String> taskNames;

        final List<String> projectTaskNames = findAllTaskNames(project.getId());
        if (filter.isEmpty()) {
            taskNames = Sets.newHashSet(projectTaskNames);
        } else {
            final Set<String> normalizedFilter = filter
                    .stream()
                    .map(TaskTrackerService::normalizeTaskName)
                    .collect(Collectors.toSet());
            taskNames = projectTaskNames
                    .stream()
                    .filter(name -> normalizedFilter.contains(normalizeTaskName(name)))
                    .collect(Collectors.toSet());
        }

        return taskNames;
    }

    public Optional<Task> findTask(Project project, String taskName) {
        return taskRepository.findTopByProjectIdAndNameOrderByIdDesc(project.getId(), taskName);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllTasksMetrics(final Long projectId) {
        Map<String, List<String>> taskStateMetrics = new HashMap<>();

        List<String> allTaskStateNames = taskRepository.getTaskNames(projectId);
        for (String taskName : allTaskStateNames) {
            Task task = taskRepository
                    .findTopByProjectIdAndNameAndStateOrderByIdDesc(projectId, taskName, Task.State.SUCCEEDED)
                    .orElseGet(() -> taskRepository.findTopByProjectIdAndNameOrderByIdDesc(projectId, taskName).get());
            List<String> metrics = findMetrics(task)
                    .stream()
                    .map(TaskMetric::getName)
                    .collect(Collectors.toList());

            taskStateMetrics.put(taskName, metrics);
        }
        return taskStateMetrics;
    }

    public Task save(final Project project, final Task task) {
        task.setProject(project);
        final Task result = taskRepository.save(task);

        eventBus.post(TaskTrackerEvent.of(task));

        return result;
    }

    public TaskMetric save(final Task task, final TaskMetric metric) {
        metric.setTask(task);

        return taskMetricRepository.save(metric);
    }

    public Warning save(final Task task, final Warning warning) {
        warning.setTask(task);

        return warningRepository.save(warning);
    }

    public TaskError save(final Task task, final TaskError taskError) {
        taskError.setTask(task);

        return taskErrorRepository.save(taskError);
    }

    public Task update(final Project project, final Task task) {
        if (isTerminalState(task.getState()) && Objects.isNull(task.getEndDate())) {
            task.setEndDate(new Date());
        }

        final Task result = taskRepository.save(task);

        eventBus.post(TaskTrackerEvent.of(task));

        return result;
    }

    private List<Task> getProjectTasks(final Project project, final Date startDate, final Date endDate) {
        return taskRepository.findByProjectIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(project.getId(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<TaskMetric> getProjectTasksMetrics(final Project project, final Set<String> metricNames, final Date startDate, final Date endDate) {
        return getProjectTasks(project, startDate, endDate)
                .stream()
                .flatMap(task -> findMetrics(task).stream())
                .filter(metric -> metricNames.isEmpty() || metricNames.contains(metric.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskType save(final TaskType type) {
        return taskTypeRepository.save(type);
    }

    public DailyTasksModel getTasksInRange(final Long projectId,
                                           final Date dateFrom,
                                           final Date dateTo,
                                           final Task.DateType dateType,
                                           final Boolean lastOnly
    ) {

        final List<Task> tasksForPeriod;
        if (dateType == Task.DateType.NOMINAL_DATE) {
            tasksForPeriod = measureTotalTime(() -> taskRepository.findTasks(projectId, dateFrom, dateTo), "find tasks");
        } else {
            tasksForPeriod = taskRepository.findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(projectId, dateFrom, dateTo);
        }

        try {
            final SimpleDateFormat dateFormat = DateTimePatterns.getDefault();
            final SimpleDateFormat datetimeFormat = DateTimePatterns.getDateTimeFormat();
            final Function<Task, String> taskToDateFunction = (task) -> {
                if (dateType == Task.DateType.NOMINAL_DATE) {
                    return dateFormat.format(task.getNominalDate());
                }
                return dateFormat.format(task.getStartDate());
            };

            final Map<String, Map<String, List<TaskModel>>> dateToTasks = Maps.newHashMap();
            for (Date date = dateFrom; date.before(dateTo) || date.equals(dateTo); date = addDays(date, 1)) {
                dateToTasks.put(dateFormat.format(date), Maps.newHashMap());
            }

            final boolean isLastOnly = Objects.nonNull(lastOnly) && lastOnly;
            if (isLastOnly) {
                // Сортируем в обратном порядке по дате завершения, для того что бы взять только одну задачу,
                // начавшуюся последней
                tasksForPeriod.sort(Comparator.comparing(Task::getStartDate).reversed());
            }

            for (final Task task : tasksForPeriod) {
                final String date = taskToDateFunction.apply(task);
                // Имя задачи -> все задачи за дату yyyy-MM-dd с таким именем
                final Map<String, List<TaskModel>> name2tasks = dateToTasks.get(date);
                final List<TaskModel> tasks = name2tasks.computeIfAbsent(task.getName(), name -> Lists.newArrayList());
                if (isLastOnly) {
                    if (tasks.isEmpty()) {
                        tasks.add(new TaskModel(datetimeFormat, task));
                    }
                } else {
                    tasks.add(new TaskModel(datetimeFormat, task));
                }
            }

            return new DailyTasksModel(dateToTasks);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Transactional(readOnly = true)
    public List<Task> filterTasks(Long projectId,
                                  String name,
                                  String status,
                                  Date startDate,
                                  Date nominalDate,
                                  Date endDate,
                                  String sortBy,
                                  Integer limit) {

        Stream<Task> projectTasks;

        if (!Objects.isNull(startDate)) {
            Date nextDayAfterStartDate = addDays(startDate, 1);
            projectTasks = taskRepository.findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(projectId, startDate, nextDayAfterStartDate).stream();
        } else if (!Objects.isNull(nominalDate)) {
            Date nextDayAfterNominalDate = addDays(nominalDate, 1);
            projectTasks = taskRepository.findByProjectIdAndNominalDateGreaterThanEqualAndNominalDateLessThan(projectId, nominalDate, nextDayAfterNominalDate).stream();
        } else if (!Objects.isNull(endDate)) {
            Date nextDayAfterEndDate = addDays(endDate, 1);
            projectTasks = taskRepository.findByProjectIdAndEndDateGreaterThanEqualAndEndDateLessThan(projectId, endDate, nextDayAfterEndDate).stream();
        } else {
            projectTasks = findTasksByProjectId(projectId).stream();
        }

        if (!Objects.isNull(name)) {
            projectTasks = projectTasks.filter(task -> task.getName().equalsIgnoreCase(name.trim()));
        }
        if (!Objects.isNull(status)) {
            projectTasks = projectTasks.filter(task -> task.getState().name().equalsIgnoreCase(status.trim()));
        }
        if (!Objects.isNull(sortBy)) {
            projectTasks = sortList(projectTasks, sortBy);
        }
        if (!Objects.isNull(limit)) {
            return projectTasks.limit(limit).collect(Collectors.toList());
        }

        return projectTasks.collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<Task> filterTasks(Long projectId,
                                  String name,
                                  String status,
                                  Map<String, Date> startDateConditions,
                                  Map<String, Date> endDateConditions,
                                  String sortBy,
                                  Integer limit,
                                  boolean tillSeconds) {

        Stream<Task> projectTasks = null;

        if (Objects.isNull(startDateConditions) && Objects.isNull(endDateConditions)) {
            projectTasks = taskRepository.findByProjectId(projectId).stream();
        } else {
            if (!Objects.isNull(startDateConditions)) {
                if (startDateConditions.containsKey("gt")) {
                    projectTasks = taskRepository.findByProjectIdAndStartDateGreaterThan(projectId, startDateConditions.get("gt")).stream();
                } else if (startDateConditions.containsKey("gteq")) {
                    projectTasks = taskRepository.findByProjectIdAndStartDateGreaterThanEqual(projectId, startDateConditions.get("gteq")).stream();
                }

                if (startDateConditions.containsKey("lt")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getStartDate()) && lt(task.getStartDate(), startDateConditions.get("lt")));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndStartDateLessThan(projectId, startDateConditions.get("lt")).stream();
                    }
                } else if (startDateConditions.containsKey("lteq")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getStartDate()) && ltOrEq(task.getStartDate(), startDateConditions.get("lteq"), tillSeconds));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndStartDateLessThanEqual(projectId, startDateConditions.get("lteq")).stream();
                    }
                }
            }

            if (!Objects.isNull(endDateConditions)) {
                if (endDateConditions.containsKey("gt")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getEndDate()) && gt(task.getEndDate(), endDateConditions.get("gt")));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndEndDateGreaterThan(projectId, endDateConditions.get("gt")).stream();
                    }
                } else if (endDateConditions.containsKey("gteq")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getEndDate()) && gtOrEq(task.getEndDate(), endDateConditions.get("gteq"), tillSeconds));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndEndDateGreaterThanEqual(projectId, endDateConditions.get("gteq")).stream();
                    }
                }

                if (endDateConditions.containsKey("lt")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getEndDate()) && lt(task.getEndDate(), endDateConditions.get("lt")));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndEndDateLessThan(projectId, endDateConditions.get("lt")).stream();
                    }
                } else if (endDateConditions.containsKey("lteq")) {
                    if (!Objects.isNull(projectTasks)) {
                        projectTasks = projectTasks.filter(task -> !Objects.isNull(task.getEndDate()) && ltOrEq(task.getEndDate(), endDateConditions.get("lteq"), tillSeconds));
                    } else {
                        projectTasks = taskRepository.findByProjectIdAndEndDateLessThanEqual(projectId, endDateConditions.get("lteq")).stream();
                    }
                }
            }
        }


        if (!Objects.isNull(name)) {
            projectTasks = projectTasks.filter(task -> task.getName().equalsIgnoreCase(name.trim()));
        }
        if (!Objects.isNull(status)) {
            projectTasks = projectTasks.filter(task -> task.getState().name().equalsIgnoreCase(status.trim()));
        }
        if (!Objects.isNull(sortBy)) {
            projectTasks = sortList(projectTasks, sortBy);
        }
        if (!Objects.isNull(limit)) {
            return projectTasks.limit(limit).collect(Collectors.toList());
        }

        return projectTasks.collect(Collectors.toList());
    }


    private Stream<Task> sortList(Stream<Task> stream, String sortBy) {

        if (sortBy.equals("name")) {
            return stream.sorted(Comparator.comparing(Task::getName, String::compareToIgnoreCase));
        } else if (sortBy.equals("status")) {
            return stream.sorted((t1, t2) -> t1.getState().name().compareToIgnoreCase(t2.getState().name()));
        } else if (sortBy.equals("startDate")) {
            return stream.sorted(Comparator.comparing(Task::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if (sortBy.equals("endDate")) {
            return stream.sorted(Comparator.comparing(Task::getEndDate, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return stream;
    }

    @Transactional
    public void delete(final Project project) {
        List<Task> tasks = taskRepository.findByProject(project);
        tasks.forEach(task -> {
            findMetrics(task).forEach(taskMetricRepository::delete);
            findErrors(task).forEach(taskErrorRepository::delete);
            findStatistics(task).forEach(statsRepository::delete);
            findWarnings(task).forEach(warningRepository::delete);

            taskRepository.delete(task);
        });
        ruleRepository.findByProject(project).forEach(ruleRepository::delete);
        projectRepository.delete(project);

    }

    @Transactional
    public ProjectSettings getOrCreateSettings(final Project project) {
        if (Objects.nonNull(project.getSettings())) {
            return project.getSettings();
        }
        final ProjectSettings settings = new ProjectSettings();
        settings.setProject(project);
        project.setSettings(settings);
        projectSettingsRepository.save(settings);

        return settings;
    }

    @Transactional(readOnly = true)
    public List<TaskType> getProjectTaskTypes(final Project project) {
        return taskTypeRepository.findByProjectId(project.getId());
    }

    @Transactional(readOnly = true)
    public List<Task> findTaskByDateAndState(final Date date, final Task.State state) {
        return taskRepository.findByDateAndState(date, state);
    }

    public long getExecutionTime(final Task task, final Date maxEndTime) {
        if (Objects.isNull(task.getStartDate())) {
            // TODO
            throw new IllegalStateException();
        }

        if (task.getStartDate().getTime() >= maxEndTime.getTime()) {
            // TODO
            throw new IllegalStateException();
        }

        final long startTime = task.getStartDate().getTime();
        final long endTime = Objects.isNull(task.getEndDate()) ? maxEndTime.getTime() : task.getEndDate().getTime();

        return TimeUnit.MILLISECONDS.toSeconds(endTime - startTime);
    }

    public long getExecutionTime(final Task task) {
        return getExecutionTime(task, new Date(Long.MAX_VALUE));
    }

    public ProjectSettings saveProjectSettings(final Project project, final ProjectSettings settings) {
        if (Objects.isNull(project.getSettings())) {
            settings.setProject(project);
            project.setSettings(settings);
        } else {
            final ProjectSettings currentSettings = project.getSettings();
            currentSettings.setAutoStatusUpdateFrequency(settings.getAutoStatusUpdateFrequency());
            currentSettings.setAutoStatusUpdate(settings.getAutoStatusUpdate());
            currentSettings.setIncidentGenerationAllowed(settings.getIncidentGenerationAllowed());
            currentSettings.setTaskDataQualityAllowed(settings.getTaskDataQualityAllowed());
            currentSettings.setTaskSynchronizationAllowed(settings.getTaskSynchronizationAllowed());
        }

        projectRepository.save(project);

        return project.getSettings();
    }

    @Transactional
    public List<Task> getProjectTasksByDate(final Project project, final Date date) {
        log.info("Get project tasks by day: project {}, date {}", project.getId(), date);

        final List<Task> tasks = taskRepository
                .findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                        project.getId(),
                        date,
                        DateUtils.addDays(date, 1)
                );

        log.info("{} tasks found", tasks.size());

        return tasks;
    }

    @Transactional
    public List<Task> getProjectTasksByMonth(final Project project, final Date date) {
        log.info("Get project tasks by month: project {}, date {}", project.getId(), date);

        final Date start = DateUtils.truncate(date, Calendar.MONTH);
        final Date end = DateUtils.addMonths(start, 1);
        final List<Task> tasks = taskRepository
                .findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                        project.getId(),
                        start,
                        end
                );

        log.info("{} tasks found", tasks.size());

        return tasks;
    }

    @Transactional
    public List<Task> getProjectTasksByYear(Project project, Date date) {
        log.info("Get project tasks by year: project {}, date {}", project.getId(), date);

        final Date start = DateUtils.truncate(date, Calendar.YEAR);
        final Date end = DateUtils.addYears(start, 1);
        final List<Task> tasks = taskRepository
                .findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                        project.getId(),
                        start,
                        end
                );

        log.info("{} tasks found", tasks.size());

        return tasks;
    }

    @Transactional
    public List<Task> getProjectTasksByRange(final Project project, final Date start, final Date end) {
        return taskRepository.findTasksInRage(project, start, end);
    }

    public List<TaskStats> saveTaskStats(final Task task, final List<TaskStats> stats) {
        stats.forEach(item -> item.setTask(task));

        return statsRepository.saveAll(stats);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasks(final Project project, final DateType dateType, final Date start, final Date end) {
        switch (dateType) {
            case NOMINAL_DATE:
                return taskRepository.findTasks(project.getId(), start, end);
            case START_DATE:
                return taskRepository.findByProjectIdAndStartDateGreaterThan(project.getId(), start);
            case END_DATE:
                return taskRepository.findByProjectIdAndEndDateLessThanEqual(project.getId(), end);
            case PERIOD:
                return taskRepository.findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(project.getId(), start, end);
        }
        return taskRepository.findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(project.getId(), start, end);
    }

    @Transactional(readOnly = true)
    public List<TaskStats> findTaskStats(final Task task) {
        return statsRepository.findByTask(task);
    }

    private boolean taskNameFilter(final TaskFilter filter, final Task task) {
        if (filter.getNames().isEmpty()) {
            return true;
        }
        return filter.getNames().stream().anyMatch(name -> name.isEmpty() || name.equalsIgnoreCase(task.getName()));
    }

    private boolean taskStatusFilter(final TaskFilter filter, final Task task) {
        if (filter.getStatuses().isEmpty()) {
            return true;
        }
        return filter.getStatuses().stream().anyMatch(status -> status.isEmpty() || status.equalsIgnoreCase(task.getState().name()));
    }

    @Transactional
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public List<Task> findTasks(final Project project, final TaskFilter filter) {
        final List<Task> tasks = findTasks(project, filter.getDateType(), filter.getFrom(), filter.getTo());
        return tasks.stream()
                .filter(task -> taskNameFilter(filter, task) && taskStatusFilter(filter, task))
                .collect(Collectors.toList());
    }

    public long renameTasks(final String originalName, final String newName) {
        log.info("Rename tasks: original name '{}', new name '{}'", originalName, newName);

        final long affected = taskRepository.renameTasks(originalName, newName);

        log.info("{} tasks renamed", affected);

        return affected;
    }

    @Transactional(readOnly = true)
    public List<Task> findUnsynchronizedTasks(final Project project) {
        return taskRepository.findUnsynchronizedProjectTasks(project, PageRequest.of(0, 25));
    }

    @Transactional(readOnly = true)
    public List<Task> findUnanalyzedTasks(final Project project) {
        List<State> states = Arrays.asList(State.SUCCEEDED, State.FAILED, State.CANCELED);
        return taskRepository.findUnanalyzedTasksProjectTasks(project, states, PageRequest.of(0, 20, Sort.by("nominalDate").ascending()));
    }

    public boolean isTerminalState(final State state) {
        Preconditions.checkArgument(state != null, "Task state must not be null!");

        if (state == State.SUCCEEDED) {
            return true;
        }

        if (state == State.FAILED) {
            return true;
        }

        if (state == State.CANCELED) {
            return true;
        }

        return false;
    }

    public boolean isTerminalState(final Task task) {
        return isTerminalState(task.getState());
    }

    @Transactional(readOnly = true)
    public List<Warning> findWarnings(final Task task) {
        return warningRepository.findByTask(task);
    }

    @Transactional(readOnly = true)
    public List<TaskMetric> findMetrics(final Task task) {
        return taskMetricRepository.findByTask(task);
    }

    @Transactional(readOnly = true)
    public List<TaskStats> findStatistics(final Task task) {
        return statsRepository.findByTask(task);
    }

    @Transactional(readOnly = true)
    public List<TaskError> findErrors(final Task task) {
        return taskErrorRepository.findByTask(task);
    }

    public LinkedHashMap<String, Integer> findTopFailedTasksStats(
            final Project project,
            final Date startDate,
            final Date endDate,
            final int top
    ) {
        List<Task> tasks = taskRepository.findTasksInRage(project, startDate, endDate);

        LinkedHashMap<String, Integer> topFailedTasks = tasks.stream()
                .filter(task -> task.getState().equals(State.FAILED))
                .collect(Collectors.groupingBy(Task::getName, Collectors.summingInt(t -> 1)))
                .entrySet().stream()
                .sorted(comparingByValue(Comparator.reverseOrder()))
                .limit(top)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return topFailedTasks;
    }

    public LinkedHashMap<String, Integer> findTopLongTasksStats(
            final Project project,
            final Date startDate,
            final Date endDate,
            final int top
    ) {
        List<Task> tasks = taskRepository.findTasksInRage(project, startDate, endDate);

        Function<Task, Long> getExecutionTime = (task) -> getExecutionTime(task);

        return tasks.stream()
                .filter(task -> task.getState().equals(State.SUCCEEDED))
                .collect(Collectors.groupingBy(Task::getName, Collectors.mapping(getExecutionTime, Collectors.toList())))
                .entrySet().stream()
                .flatMap((entry) -> {
                    Map<String, Integer> m = Maps.newHashMap();
                    m.put(entry.getKey(), (int) Quantiles.median().compute(entry.getValue().stream().mapToLong(i -> i).toArray()));
                    return m.entrySet().stream();
                })
                .sorted(comparingByValue(Comparator.reverseOrder()))
                .limit(top)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    }

    public LinkedHashMap<Date, Integer> getTasksDistribution(
            final Project project,
            final Date startDate,
            final Date endDate,
            final Frequency frequency
    ) {
        List<Task> tasks;
        if (frequency == Frequency.HOUR) {
            tasks = taskRepository.findTasksInRageTruncHour(project, startDate, endDate);
        } else {
            tasks = taskRepository.findTasksInRage(project, startDate, endDate);
        }

        DateTaskGrouper taskGrouper = new DateTaskGrouper.DateTaskGrouperBuilder()
                .frequency(frequency)
                .start(startDate)
                .end(endDate)
                .dateType(DateType.START_DATE)
                .build();

        return taskGrouper.group(tasks).entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public List<TasksReportModel> groupTasksByNameAndDate(
            final Project project,
            final TaskFilter filter,
            final Set<State> taskStatuses,
            final Boolean lastTaskOnly
    ) {
        final List<Task> tasks = findTasks(project, filter);
        final Map<String, Map<Date, List<Task>>> groupedTasks = NameDateTaskGrouper
                .builder()
                .start(filter.getFrom())
                .end(filter.getTo())
                .dateType(filter.getDateType())
                .taskNames(filter.getNames())
                .statuses(taskStatuses)
                .lastTaskOnly(lastTaskOnly)
                .build()
                .group(tasks);

        final List<TasksReportModel> result = groupedTasks.entrySet().stream().map(entry -> {
            final TasksReportModel periodTasksModel = new TasksReportModel();
            periodTasksModel.setName(entry.getKey());
            periodTasksModel.setDates(entry.getValue().entrySet().stream().map(dateWithTasks -> {
                final DateTasks dateTask = new DateTasks();
                dateTask.setDate(DateTimePatterns.toYyyyMmDd(dateWithTasks.getKey()));
                dateTask.setTasks(dateWithTasks.getValue().stream().map(task -> {
                    final TaskModel model = new TaskModel(task);
                    if (filter.isWithMetrics()) {
                        model.setMetrics(findMetrics(task).stream().map(TaskMetricModel::new).collect(Collectors.toList()));
                    }
                    if (filter.isWithStatistics()) {
                        TaskStatsMapper taskStatsMapper = new TaskStatsMapper();
                        model.setStatistics(findStatistics(task).stream().map(taskStatsMapper::to).collect(Collectors.toList()));
                    }
                    if (filter.isWithErrors()) {
                        model.setErrors(findErrors(task).stream().map(TaskErrorModel::new).collect(Collectors.toList()));
                    }
                    if (filter.isWithWarnings()) {
                        model.setWarnings(findWarnings(task).stream().map(WarningModel::new).collect(Collectors.toList()));
                    }
                    return model;
                }).collect(Collectors.toList()));
                return dateTask;
            }).collect(Collectors.toList()));
            return periodTasksModel;
        }).collect(Collectors.toList());

        return result;
    }

    public List<TopChart.TopElement> findTopErrors(
            final Project project,
            final Date startDate,
            final Date endDate,
            final int top
    ) {
        List<Task> tasks = taskRepository.findTasksInRage(project, startDate, endDate);

        return tasks.stream()
                .filter(task -> task.getState().equals(State.FAILED))
                .flatMap(task -> findErrors(task).stream().filter(taskError -> Objects.nonNull(taskError.getType())))
                .collect(Collectors.groupingBy(TaskError::getType, Collectors.summingInt(t -> 1)))
                .entrySet().stream()
                .map(entry -> {
                    TopChart.TopElement errorElement = new TopChart.TopElement();
                    errorElement.setName(entry.getKey().name());
                    errorElement.setCounter(entry.getValue());

                    return errorElement;
                })
                .sorted(Comparator.comparing(TopChart.TopElement::getCounter).reversed())
                .limit(top)
                .collect(Collectors.toList());
    }

    public List<TopChart.TopWarningElement> findTopWarnings(
            final Project project,
            final Date startDate,
            final Date endDate,
            final int top
    ) {
        List<Task> tasks = taskRepository.findTasksInRage(project, startDate, endDate);

        return tasks.stream()
                .flatMap(task -> findWarnings(task).stream())
                .collect(Collectors.groupingBy(Warning::getMessage))
                .entrySet().stream()
                .map(entry -> {
                    TopChart.TopWarningElement warningElement = new TopChart.TopWarningElement();
                    warningElement.setName(entry.getKey());
                    warningElement.setCounter(entry.getValue().size());
                    warningElement.setTaskName(entry.getValue().get(0).getTask().getName());

                    return warningElement;
                })
                .sorted(Comparator.comparing(TopChart.TopWarningElement::getCounter).reversed())
                .limit(top)
                .collect(Collectors.toList());
    }

    public List<Warning> findWarnings(
            final Project project,
            final Date from,
            final Date to,
            final String taskName
    ) {
        List<Warning> warnings = warningRepository.findWarningsInRange(project, from, to);
        if (Objects.nonNull(taskName) && !taskName.trim().isEmpty()) {
            return warnings
                    .stream()
                    .filter(warning -> warning.getTask().getName().equalsIgnoreCase(taskName))
                    .collect(Collectors.toList());
        }
        return warnings;
    }

    public List<TaskStats> findLastNSucceededTasksStats(
            final Project project,
            final Task task,
            final String statsColumnName,
            final Integer statsNumber
    ) {
        DateFormat df = getDefault();
        Function<Task, String> taskToNominalDate = t -> df.format(DateUtils.truncate(t.getNominalDate(), Calendar.DAY_OF_MONTH));

        return taskRepository.findLastNTasksByProjectAndNominalDateAndState(project.getId(), task.getName(), State.SUCCEEDED.name(), task.getNominalDate(), 2 * statsNumber)
                .stream()
                .collect(Collectors.groupingBy(taskToNominalDate,
                        Collectors.reducing(BinaryOperator.maxBy(Comparator.comparing(Task::getStartDate)))))
                .entrySet()
                .stream()
                .map(entry -> entry.getValue().get())
                .filter(tsk -> findWarnings(tsk).isEmpty())
                .sorted(Comparator.comparing(Task::getNominalDate).reversed())
                .limit(statsNumber)
                .flatMap(t -> findStatistics(t).stream())
                .filter(taskStats -> taskStats.getColumn().equalsIgnoreCase(statsColumnName))
                .collect(Collectors.toList());
    }

    public List<KeyValueModel> getTaskStats(
            final Project project,
            final String taskName,
            final Date startDate,
            final Date endDate,
            final String tableName,
            final String columnName,
            final DataQualityCondition.Metric metric
    ) {
        DateFormat df = getDefault();
        Function<Task, String> taskToNominalDate = t -> df.format(DateUtils.truncate(t.getNominalDate(), Calendar.DAY_OF_MONTH));

        return taskRepository.findTasks(project.getId(), startDate, endDate).stream()
                .filter(task -> task.getName().equalsIgnoreCase(taskName) && task.getState() == State.SUCCEEDED)
                .collect(Collectors.groupingBy(taskToNominalDate,
                        Collectors.reducing(BinaryOperator.maxBy(Comparator.comparing(Task::getStartDate)))))
                .entrySet()
                .stream()
                .map(entry -> entry.getValue().get())
                .flatMap(task -> findStatistics(task).stream())
                .filter(taskStats ->
                        taskStats.getColumn().equals(tableName + "." + columnName) &&
                                Objects.nonNull(taskStats.getMetric(metric))
                )
                .sorted(Comparator.comparing(TaskStats::getTask, Comparator.comparing(Task::getNominalDate)))
                .map(taskStats -> new KeyValueModel(df.format(taskStats.getTask().getNominalDate()), taskStats.getMetric(metric)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteWarningsAndSetUnanalyzed(final Task task, final Boolean resetAnalyzedFlag) {
        findWarnings(task).forEach(warning -> {
            if (StringUtils.startsWith(warning.getMessage(), "Количество строк")
                    || StringUtils.startsWith(warning.getMessage(), "Для колонки")
                    || StringUtils.startsWith(warning.getMessage(), "Condition")
                    || StringUtils.startsWith(warning.getMessage(), "Metric")) {
                warningRepository.delete(warning);
            }
        });

        if (Objects.nonNull(resetAnalyzedFlag) && resetAnalyzedFlag) {
            task.setAnalyzed(false);
            taskRepository.save(task);
        }
    }

    public void deleteWarningsAndSetUnanalyzed(
            final Project project,
            final Date from,
            final Date to,
            final String taskName,
            final Boolean resetAnalyzedFlag
    ) {
        List<Task> tasks = findTasks(project, DateType.NOMINAL_DATE, from, to)
                .stream()
                .filter(task -> {
                    if (StringUtils.isNotBlank(taskName)) {
                        return task.getName().equalsIgnoreCase(taskName);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        tasks.forEach(task -> deleteWarningsAndSetUnanalyzed(task, resetAnalyzedFlag));
    }

}
