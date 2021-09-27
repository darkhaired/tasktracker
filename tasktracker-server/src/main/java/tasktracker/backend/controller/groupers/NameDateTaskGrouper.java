package tasktracker.backend.controller.groupers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.Task.DateType;
import tasktracker.backend.model.Task.State;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public final class NameDateTaskGrouper {
    private final Date start;
    private final Date end;
    private final Frequency frequency;
    private final DateType dateType;
    private final Set<String> taskNames;
    private final Set<State> statuses;
    private final Boolean lastTaskOnly;

    private NameDateTaskGrouper(
            final Date start,
            final Date end,
            final Frequency frequency,
            final DateType dateType,
            final Set<String> taskNames,
            final Set<State> statuses,
            final Boolean lastTaskOnly
    ) {
        this.start = start;
        this.end = end;
        this.frequency = frequency;
        this.dateType = dateType;
        this.taskNames = taskNames;
        this.statuses = statuses;
        this.lastTaskOnly = lastTaskOnly;
    }

    public static NameDateTaskGrouperBuilder builder() {
        return new NameDateTaskGrouperBuilder();
    }

    public static void main(String... args) throws ParseException {
//        dailyGrouping();
//        monthlyGrouping();
//        yearlyGrouping();
        System.out.println(
                Sets.difference(
                        Sets.newHashSet("a", "b", "c"),
                        Sets.newHashSet()
                )
        );
    }

    public static void dailyGrouping() throws ParseException {
        final NameDateTaskGrouper grouper = builder()
                .start(new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-01"))
                .end(new SimpleDateFormat("yyyy-MM-dd").parse("2020-04-01"))
                .daily().build();

//        System.out.println(grouper.datesRange());
        final List<Task> tasks = new ArrayList<>();
        tasks.add(task("A", "2020-03-11", "2020-03-01"));
        tasks.add(task("A", "2020-03-12", "2020-03-01"));
        tasks.add(task("A", "2020-03-13", "2020-03-01"));

        tasks.add(task("A", "2020-03-13", "2020-03-10"));
        tasks.add(task("A", "2020-03-13", "2020-03-10"));

        tasks.add(task("B", "2020-03-13", "2020-03-10"));

        System.out.println(grouper.group(tasks).get("A"));
    }

    public static void monthlyGrouping() throws ParseException {
        final NameDateTaskGrouper grouper = builder()
                .start(new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-01"))
                .end(new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-10"))
                .monthly()
                .build();

        System.out.println(grouper.datesRange());

        final List<Task> tasks = new ArrayList<>();
        tasks.add(task("A", "2020-03-11", "2020-03-01"));
        tasks.add(task("A", "2020-03-12", "2020-03-01"));
        tasks.add(task("A", "2020-03-13", "2020-03-01"));

        tasks.add(task("A", "2020-03-13", "2020-03-10"));
        tasks.add(task("A", "2020-03-13", "2020-03-10"));

        tasks.add(task("B", "2020-03-13", "2020-03-10"));

        tasks.add(task("C", "2020-04-13", "2020-04-10"));

        System.out.println(grouper.group(tasks));

    }

    public static void yearlyGrouping() throws ParseException {
        final NameDateTaskGrouper grouper = builder()
                .start(new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-01"))
                .end(new SimpleDateFormat("yyyy-MM-dd").parse("2021-04-10"))
                .yearly()
                .build();

        System.out.println(grouper.datesRange());

        final List<Task> tasks = new ArrayList<>();
        tasks.add(task("A", "2020-03-11", "2020-03-01"));
        tasks.add(task("A", "2020-03-12", "2020-03-01"));
        tasks.add(task("A", "2020-03-13", "2020-03-01"));

        tasks.add(task("A", "2020-03-13", "2020-03-10"));
        tasks.add(task("A", "2020-03-13", "2020-03-10"));

        tasks.add(task("B", "2020-03-13", "2020-03-10"));

        tasks.add(task("C", "2020-04-13", "2020-04-10"));

        System.out.println(grouper.group(tasks));

    }

    static Task task(final String name, final String start, final String nominal) {
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            final Task task = new Task();
            task.setName(name);
            task.setStartDate(format.parse(start));
            task.setNominalDate(format.parse(nominal));
            return task;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Function<String, Map<Date, List<Task>>> populate(final List<Date> dates) {
        final Comparator<Task> cmp = Comparator.comparing(Task::getStartDate);
        return (name) -> {
            final TreeMap<Date, List<Task>> dateToTasks = new TreeMap<>();
            dates.forEach(date -> dateToTasks.put(date, new SortedList<>(cmp)));
            return dateToTasks;
        };
    }

    public Map<String, Map<Date, List<Task>>> group(final List<Task> tasks) {
        final Function<Task, Date> dateExtractor = task -> {
            if (dateType == DateType.NOMINAL_DATE) {
                return task.getNominalDate();
            }
            return task.getStartDate();
        };

        // Task name is a key of map
        final Map<String, Map<Date, List<Task>>> result = Maps.newHashMap();
        final List<Date> dates = datesRange();

        for (final String taskName : taskNames) {
            result.computeIfAbsent(taskName, populate(dates));
        }

        final long truncatedStart = DateUtils.truncate(start, Calendar.DAY_OF_MONTH).getTime();
        final long truncatedEnd = DateUtils.truncate(end, Calendar.DAY_OF_MONTH).getTime();

        for (final Task task : tasks) {
            final Date truncatedTaskDate = DateUtils.truncate(dateExtractor.apply(task), Calendar.DAY_OF_MONTH);
            result
                    .computeIfAbsent(task.getName(), populate(dates))
                    .computeIfPresent(truncatedTaskDate, (date, taskList) -> {
                        final long taskDate = truncatedTaskDate.getTime();
                        if (taskDate >= truncatedStart && taskDate <= truncatedEnd) {
                            taskList.add(task);
                        }
                        return taskList;
                    });
        }

        if (lastTaskOnly) {
            result.forEach((taskName, dateToTasks) -> {
                dateToTasks.forEach((date, tasksList) -> {
                    if (!tasksList.isEmpty()) {
                        dateToTasks.replace(date, Lists.newArrayList(tasksList.get(tasksList.size() - 1)));
                    }
                });
            });
        }

        if (!statuses.isEmpty()) {
            final Map<String, Map<Date, List<Task>>> statusesFilteredResult = Maps.newHashMap();

            result.forEach((taskName, dateToTasksList) -> {
                Map<Date, List<Task>> dateToStatusMatchTasksList = Maps.newHashMap();

                dateToTasksList.forEach((date, tasksList) -> {
                    if (!tasksList.isEmpty()) {
                        final Task lastTask = tasksList.get(tasksList.size() - 1);
                        if (statuses.contains(lastTask.getState())) {
                            dateToStatusMatchTasksList.put(date, tasksList);
                        }
                    }
                });
                if (!dateToStatusMatchTasksList.isEmpty()) {
                    statusesFilteredResult
                            .computeIfAbsent(taskName, populate(dates))
                            .putAll(dateToStatusMatchTasksList);
                }
            });

            return statusesFilteredResult;
        }

        return result;
    }

    public List<Date> datesRange() {
        final List<Date> result = Lists.newArrayList();
        final Function<Date, Date> generator = date -> {
            if (frequency == Frequency.HOUR) {
                return DateUtils.addHours(date, 1);
            }
            if (frequency == Frequency.DAY) {
                return DateUtils.addDays(date, 1);
            }
            if (frequency == Frequency.WEEK) {
                return DateUtils.addWeeks(date, 1);
            }
            if (frequency == Frequency.MONTH) {
                return DateUtils.addMonths(date, 1);
            }
            if (frequency == Frequency.YEAR) {
                return DateUtils.addYears(date, 1);
            }
            throw new RuntimeException();
        };

        for (Date date = start; date.before(end) || date.equals(end); date = generator.apply(date)) {
            result.add(DateUtils.truncate(date, Calendar.DAY_OF_MONTH));
        }

        result.sort(Date::compareTo);

        return result;
    }

    public static class NameDateTaskGrouperBuilder {
        private Date start = new Date();
        private Date end = new Date();
        private Frequency frequency = Frequency.DAY;
        private Task.DateType dateType = DateType.NOMINAL_DATE;
        private Set<String> taskNames = Collections.emptySet();
        private Set<State> statuses = Sets.newHashSet(State.values());
        private Boolean lastTaskOnly = Boolean.FALSE;

        public NameDateTaskGrouper build() {
            return new NameDateTaskGrouper(
                    start,
                    end,
                    frequency,
                    dateType,
                    taskNames,
                    statuses,
                    lastTaskOnly
            );
        }

        public NameDateTaskGrouperBuilder start(final Date start) {
            this.start = start;
            return this;
        }

        public NameDateTaskGrouperBuilder end(final Date end) {
            this.end = end;
            return this;
        }

        public NameDateTaskGrouperBuilder daily() {
            this.frequency = Frequency.DAY;
            return this;
        }

        public NameDateTaskGrouperBuilder monthly() {
            this.frequency = Frequency.MONTH;
            return this;
        }

        public NameDateTaskGrouperBuilder yearly() {
            this.frequency = Frequency.YEAR;
            return this;
        }

        public NameDateTaskGrouperBuilder dateType(final Task.DateType dateType) {
            this.dateType = dateType;
            return this;
        }

        public NameDateTaskGrouperBuilder taskNames(final Set<String> taskNames) {
            this.taskNames = taskNames;
            return this;
        }

        public NameDateTaskGrouperBuilder statuses(final Set<State> statues) {
            this.statuses = statues;
            return this;
        }

        public NameDateTaskGrouperBuilder lastTaskOnly(final Boolean lastTaskOnly) {
            this.lastTaskOnly = lastTaskOnly;
            return this;
        }
    }

    public static class SortedList<E> extends ArrayList<E> {
        private final Comparator<E> comparator;

        public SortedList(final Comparator<E> comparator) {
            this.comparator = comparator;
        }

        @Override
        public boolean add(E e) {
            final boolean result = super.add(e);
            sort(comparator);
            return result;
        }
    }
}
