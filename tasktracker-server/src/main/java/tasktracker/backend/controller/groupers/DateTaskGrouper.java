package tasktracker.backend.controller.groupers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.time.DateUtils;
import tasktracker.backend.model.Task;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public class DateTaskGrouper {

    private final Date start;
    private final Date end;
    private final Frequency frequency;
    private final Task.DateType dateType;

    private DateTaskGrouper(Date start, Date end, Frequency frequency, Task.DateType dateType) {
        this.start = start;
        this.end = end;
        this.frequency = frequency;
        this.dateType = dateType;
    }

    public static Date getNearestLowerDate(final TreeSet<Date> dates, final Date date) {
        return dates.floor(date);
    }

    public static void main(String[] args) throws Exception {
        DateTaskGrouper taskGrouper = new DateTaskGrouperBuilder()
                .weekly()
                .start(new SimpleDateFormat("yyyy-MM-dd").parse("2020-05-01"))
                .end(new SimpleDateFormat("yyyy-MM-dd").parse("2020-05-23"))
                .build();

        List<Date> dates = taskGrouper.datesRange();
        TreeSet<Date> sortedDates = new TreeSet<>(dates);

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-29");
        Date nearest = getNearestLowerDate(sortedDates, date);
        System.out.println("dates = " + dates);
        System.out.println("nearest = " + nearest);
    }

    public Map<Date, Integer> group(List<Task> tasks) {
        final Function<Task, Date> dateExtractor = task -> {
            if (dateType == Task.DateType.NOMINAL_DATE) {
                return task.getNominalDate();
            }
            return task.getStartDate();
        };
        final Function<Date, Date> dateTruncator = date -> {
            if (frequency == Frequency.HOUR) {
                return DateUtils.truncate(date, Calendar.HOUR);
            }
            return DateUtils.truncate(date, Calendar.DAY_OF_MONTH);
        };

        Map<Date, Integer> result = Maps.newHashMap();

        final List<Date> dates = datesRange();
        TreeSet<Date> sortedDates = new TreeSet<>(dates);

        dates.forEach(date ->
                result.computeIfAbsent(date, k -> 0)
        );

        long truncatedStart = dateTruncator.apply(start).getTime();
        long truncatedEnd = dateTruncator.apply(end).getTime();

        tasks.forEach((task) -> {
            final long truncatedTaskStartDate = dateTruncator.apply(dateExtractor.apply(task)).getTime();
            if (truncatedTaskStartDate >= truncatedStart && truncatedTaskStartDate <= truncatedEnd) {
                Date nearestDate = getNearestLowerDate(sortedDates, dateExtractor.apply(task));
                result.computeIfPresent(nearestDate, (k, v) -> v + 1);
            }
        });

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
            result.add(DateUtils.truncate(date, Calendar.HOUR));
        }

        result.sort(Date::compareTo);

        return result;
    }

    public static class DateTaskGrouperBuilder {
        private Date start = new Date();
        private Date end = new Date();
        private Frequency frequency = Frequency.DAY;
        private Task.DateType dateType = Task.DateType.NOMINAL_DATE;

        public DateTaskGrouper build() {
            return new DateTaskGrouper(
                    start,
                    end,
                    frequency,
                    dateType
            );
        }

        public DateTaskGrouperBuilder frequency(final Frequency frequency) {
            this.frequency = frequency;
            return this;
        }

        public DateTaskGrouperBuilder start(final Date start) {
            this.start = start;
            return this;
        }

        public DateTaskGrouperBuilder end(final Date end) {
            this.end = end;
            return this;
        }

        public DateTaskGrouperBuilder hourly() {
            this.frequency = Frequency.HOUR;
            return this;
        }

        public DateTaskGrouperBuilder daily() {
            this.frequency = Frequency.DAY;
            return this;
        }

        public DateTaskGrouperBuilder weekly() {
            this.frequency = Frequency.WEEK;
            return this;
        }

        public DateTaskGrouperBuilder monthly() {
            this.frequency = Frequency.MONTH;
            return this;
        }

        public DateTaskGrouperBuilder yearly() {
            this.frequency = Frequency.YEAR;
            return this;
        }

        public DateTaskGrouperBuilder dateType(final Task.DateType dateType) {
            this.dateType = dateType;
            return this;
        }
    }
}
