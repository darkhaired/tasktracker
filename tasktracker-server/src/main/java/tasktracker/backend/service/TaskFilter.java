package tasktracker.backend.service;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static tasktracker.backend.model.Task.DateType;

public class TaskFilter {
    private Date from = new Date();
    private Date to = new Date();
    private Set<String> names = Collections.emptySet();
    private Set<String> statuses = Collections.emptySet();
    private int limit = 100;
    private DateType dateType = DateType.NOMINAL_DATE;
    private boolean withMetrics = false;
    private boolean withErrors = false;
    private boolean withStatistics = false;
    private boolean withWarnings = false;

    public static TaskFilter of() {
        return new TaskFilter();
    }

    public Date getFrom() {
        return from;
    }

    public TaskFilter setFrom(Date from) {
        this.from = from;
        return this;
    }

    public Date getTo() {
        return to;
    }

    public TaskFilter setTo(Date to) {
        this.to = to;
        return this;
    }

    public Set<String> getNames() {
        return names;
    }

    public TaskFilter setNames(Set<String> names) {
        this.names = names;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public TaskFilter setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public DateType getDateType() {
        return dateType;
    }

    public TaskFilter setDateType(DateType dateType) {
        this.dateType = dateType;
        return this;
    }

    public Set<String> getStatuses() {
        return statuses;
    }

    public TaskFilter setStatuses(Set<String> statuses) {
        this.statuses = statuses;
        return this;
    }

    public boolean isWithMetrics() {
        return withMetrics;
    }

    public TaskFilter setWithMetrics(boolean withMetrics) {
        this.withMetrics = withMetrics;
        return this;
    }

    public boolean isWithErrors() {
        return withErrors;
    }

    public TaskFilter setWithErrors(boolean withErrors) {
        this.withErrors = withErrors;
        return this;
    }

    public boolean isWithStatistics() {
        return withStatistics;
    }

    public TaskFilter setWithStatistics(boolean withStatistics) {
        this.withStatistics = withStatistics;
        return this;
    }

    public boolean isWithWarnings() {
        return withWarnings;
    }

    public TaskFilter setWithWarnings(boolean withWarnings) {
        this.withWarnings = withWarnings;
        return this;
    }
}
