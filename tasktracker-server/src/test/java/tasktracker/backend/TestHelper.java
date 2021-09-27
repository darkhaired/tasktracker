package tasktracker.backend;

import tasktracker.backend.model.*;

import java.util.List;

import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateString;

public class TestHelper {
    public static Task task(
            final Project project,
            final String name,
            final String state,
            final String nominalDate,
            final String startDate
    ) {
        try {
            final Task task = new Task();
            task.setProject(project);
            task.setState(Task.State.valueOf(state));
            task.setName(name);
            task.setNominalDate(getDateFromFormattedDateString(nominalDate));
            task.setStartDate(getDateFromFormattedDateString(startDate));
            return task;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Task task(
            final Project project,
            final String name,
            final String state,
            final String nominalDate,
            final String startDate,
            final String endDate
    ) {
        try {
            final Task task = new Task();
            task.setProject(project);
            task.setState(Task.State.valueOf(state));
            task.setName(name);
            task.setNominalDate(getDateFromFormattedDateString(nominalDate));
            task.setStartDate(getDateFromFormattedDateString(startDate));
            task.setEndDate(getDateFromFormattedDateString(endDate));
            return task;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TaskError error(
            final Task task,
            final String reason
    ) {
        TaskError error = new TaskError();
        error.setReason(reason);
        error.setTask(task);

        return error;
    }

    public static TaskError error(
            final Task task,
            final String reason,
            final TaskError.Type type
    ) {
        TaskError error = error(task, reason);
        error.setType(type);

        return error;
    }

    public static Warning warning(
            final Task task,
            final String message
    ) {
        Warning warning = new Warning();
        warning.setMessage(message);
        warning.setTask(task);

        return warning;
    }

    public static DataQualityRule dqRule(
            final Long id,
            final Project project,
            final String tableName,
            final String taskName,
            final String caption,
            final List<DataQualityCondition> conditions
    ) {
        DataQualityRule rule = new DataQualityRule();
        rule.setId(id);
        rule.setProject(project);
        rule.setTableName(tableName);
        rule.setTaskName(taskName);
        rule.setCaption(caption);
        rule.setConditions(conditions);

        return rule;
    }

}
