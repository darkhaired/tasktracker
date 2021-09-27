package tasktracker.backend.dq;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tasktracker.backend.controller.exception.ApiException;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskStats;

import java.util.List;

@Getter
@Setter
public abstract class DataQualityFunction {
    private String name;
    private String description;
    private int argsNum;
    private List<DataQualityFunctionArgument> arguments;

    abstract void setProject(final Project project);

    abstract void setTask(final Task task);

    abstract void setTaskStats(final TaskStats taskStats);

    abstract void setMetric(final DataQualityCondition.Metric metric);

    void argumentsCheck(List<String> args) throws ApiException.InvalidDataQualityConditionException {

    }


    public enum ArgumentType {
        STRING,
        NUMBER,
        BOOLEAN
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class DataQualityFunctionArgument {
        private String name;
        private ArgumentType argumentType;
        private List<String> fixedValues;
        private String placeholder;
        private String description;
    }
}
