package tasktracker.backend.dq;


import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import tasktracker.backend.controller.exception.ApiException;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskStats;
import tasktracker.backend.service.TaskTrackerService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;


@Component
public class ExpressionsEvaluator {
    public final Logger logger = LoggerFactory.getLogger(getClass());

    private final StandardEvaluationContext evaluationContext;
    private final ExpressionParser expressionParser;
    private final List<DataQualityFunction> functions;

    private final TaskTrackerService taskTrackerService;

    @Autowired
    public ExpressionsEvaluator(final TaskTrackerService taskTrackerService) {
        this.taskTrackerService = taskTrackerService;

        this.evaluationContext = new StandardEvaluationContext();
        this.expressionParser = new SpelExpressionParser();
        this.functions = new ArrayList<>();

        try {
            initFunctions();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        descriptiveStatistics.addValue(40L);
        descriptiveStatistics.addValue(60L);
        descriptiveStatistics.addValue(50L);

        double mean = descriptiveStatistics.getMean();
        double median = descriptiveStatistics.getPercentile(50);

        System.out.println("mean = " + mean);
        System.out.println("median = " + median);

        Boolean calculateDelta = Boolean.valueOf("false");
        System.out.println("calculateDelta = " + calculateDelta);
    }

    private void setProject(final Project project) {
        for (DataQualityFunction dataQualityFunction : functions) {
            dataQualityFunction.setProject(project);
        }
    }

    private void setTask(final Task task) {
        for (DataQualityFunction dataQualityFunction : functions) {
            dataQualityFunction.setTask(task);
        }
    }

    private void setTaskStats(final TaskStats taskStats) {
        for (DataQualityFunction dataQualityFunction : functions) {
            dataQualityFunction.setTaskStats(taskStats);
        }
    }

    private void setMetric(final DataQualityCondition.Metric metric) {
        for (DataQualityFunction dataQualityFunction : functions) {
            dataQualityFunction.setMetric(metric);
        }
    }

    private void initFunctions() throws NoSuchMethodException {
        IsAboveFunction isAboveFunction = new IsAboveFunction(taskTrackerService);
        evaluationContext.setVariable("is_above", isAboveFunction);
        functions.add(isAboveFunction);

        IsBelowFunction isBelowFunction = new IsBelowFunction(taskTrackerService);
        evaluationContext.setVariable("is_below", isBelowFunction);
        functions.add(isBelowFunction);

        WithinRangeFunction withinRangeFunction = new WithinRangeFunction(taskTrackerService);
        evaluationContext.setVariable("is_within_range", withinRangeFunction);
        functions.add(withinRangeFunction);

        OutsideRangeFunction outsideRangeFunction = new OutsideRangeFunction(taskTrackerService);
        evaluationContext.setVariable("is_outside_range", outsideRangeFunction);
        functions.add(outsideRangeFunction);

        ConfidenceIntervalSigm confidenceIntervalSigm = new ConfidenceIntervalSigm(taskTrackerService);
        evaluationContext.setVariable("confidence_interval_sigma", confidenceIntervalSigm);
        functions.add(confidenceIntervalSigm);
    }

    public List<DataQualityFunction> getFunctions() {
        return functions;
    }

    public synchronized ExpressionEvaluatorResponse isFullfilled(
            final Project project,
            final Task task,
            final TaskStats taskStats,
            final DataQualityCondition condition
    ) throws ParseException, EvaluationException {
        setProject(project);
        setTask(task);
        setTaskStats(taskStats);
        setMetric(condition.getMetric());
        String expression = "#" + condition.getExpression();
        ExpressionEvaluatorResponse result = (ExpressionEvaluatorResponse) expressionParser.parseExpression(expression).getValue(evaluationContext);
        return result;
    }

    public synchronized void tryParse(
            final String expr
    ) throws ParseException {
        String expression = "#" + expr;
        Expression e = expressionParser.parseExpression(expression);

    }

    public static class IsAboveFunction extends DataQualityFunction implements Function<Object[], ExpressionEvaluatorResponse> {
        private final TaskTrackerService taskTrackerService;
        private Project project;
        private Task task;
        private TaskStats taskStats;
        private DataQualityCondition.Metric metric;

        public IsAboveFunction(TaskTrackerService taskTrackerService) {
            this.taskTrackerService = taskTrackerService;
            setName("is_above");
            setArgsNum(1);
            setDescription("");
            setArguments(Lists.newArrayList(
                    new DataQualityFunctionArgument(
                            "x",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "3000000",
                            "Min value"
                    )));
        }

        @Override
        public void setProject(Project project) {
            this.project = project;
        }

        @Override
        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void setTaskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
        }

        @Override
        public void setMetric(DataQualityCondition.Metric metric) {
            this.metric = metric;
        }

        @Override
        public ExpressionEvaluatorResponse apply(Object[] args) {
            if (args.length != 1) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function is_above takes only 1 parameter, not %i", args.length));
            }
            Double metricValue = taskStats.getMetric(metric);
            if (Objects.isNull(metricValue)) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("Metric %s for TaskStats %s is null", metric.name(), taskStats.getId()));
            }
            Double n = new Double(args[0].toString());

            if (metricValue > n) {
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            } else {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], metric %s = %f is not above %f", taskStats.getId(), metric.name(), metricValue, n));
            }
        }
    }

    static class IsBelowFunction extends DataQualityFunction implements Function<Object[], ExpressionEvaluatorResponse> {
        private final TaskTrackerService taskTrackerService;
        private Project project;
        private Task task;
        private TaskStats taskStats;
        private DataQualityCondition.Metric metric;

        public IsBelowFunction(TaskTrackerService taskTrackerService) {
            this.taskTrackerService = taskTrackerService;

            setName("is_below");
            setArgsNum(1);
            setDescription("");
            setArguments(Lists.newArrayList(
                    new DataQualityFunctionArgument(
                            "x",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "3000000",
                            "Max value")
            ));
        }

        @Override
        public void setProject(Project project) {
            this.project = project;
        }

        @Override
        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void setTaskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
        }

        @Override
        public void setMetric(DataQualityCondition.Metric metric) {
            this.metric = metric;
        }

        @Override
        public ExpressionEvaluatorResponse apply(Object[] args) {
            if (args.length != 1) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function is_below takes only 1 parameter, not %i", args.length));
            }
            Double metricValue = taskStats.getMetric(metric);
            if (Objects.isNull(metricValue)) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("Metric %s for TaskStats %s is null", metric.name(), taskStats.getId()));
            }
            Double n = new Double(args[0].toString());

            if (metricValue < n) {
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            } else {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], metric %s = %f is not below %f", taskStats.getId(), metric.name(), metricValue, n));
            }
        }
    }

    static class WithinRangeFunction extends DataQualityFunction implements Function<Object[], ExpressionEvaluatorResponse> {
        private final TaskTrackerService taskTrackerService;
        private Project project;
        private Task task;
        private TaskStats taskStats;
        private DataQualityCondition.Metric metric;

        public WithinRangeFunction(TaskTrackerService taskTrackerService) {
            this.taskTrackerService = taskTrackerService;
            setName("is_within_range");
            setArgsNum(2);
            setDescription("Range function");
            setArguments(Lists.newArrayList(
                    new DataQualityFunctionArgument(
                            "x",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "1000",
                            "Min value"),
                    new DataQualityFunctionArgument(
                            "y",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "3000",
                            "Max value")
            ));
        }

        @Override
        public void setProject(Project project) {
            this.project = project;
        }

        @Override
        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void setTaskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
        }

        @Override
        public void setMetric(DataQualityCondition.Metric metric) {
            this.metric = metric;
        }

        @Override
        public ExpressionEvaluatorResponse apply(Object[] args) {
            if (args.length != 2) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function is_within_range takes 2 parameters, not %i", args.length));
            }
            Double metricValue = taskStats.getMetric(metric);
            if (Objects.isNull(metricValue)) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("Metric %s for TaskStats %s is null", metric.name(), taskStats.getId()));
            }
            Double min = new Double(args[0].toString());
            Double max = new Double(args[1].toString());

            if (min <= metricValue && metricValue <= max) {
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            } else {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], metric %s = %f is not between %f and %f", taskStats.getId(), metric.name(), metricValue, min, max));
            }
        }
    }

    static class OutsideRangeFunction extends DataQualityFunction implements Function<Object[], ExpressionEvaluatorResponse> {
        private final TaskTrackerService taskTrackerService;
        private Project project;
        private Task task;
        private TaskStats taskStats;
        private DataQualityCondition.Metric metric;

        public OutsideRangeFunction(TaskTrackerService taskTrackerService) {
            this.taskTrackerService = taskTrackerService;
            setName("is_outside_range");
            setArgsNum(2);
            setDescription("Out of range function");
            setArguments(Lists.newArrayList(
                    new DataQualityFunctionArgument(
                            "x",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "1000",
                            "Min value"),
                    new DataQualityFunctionArgument(
                            "y",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "3000",
                            "Max value")
            ));
        }

        @Override
        public void setProject(Project project) {
            this.project = project;
        }

        @Override
        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void setTaskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
        }

        @Override
        public void setMetric(DataQualityCondition.Metric metric) {
            this.metric = metric;
        }

        @Override
        public ExpressionEvaluatorResponse apply(Object[] args) {
            if (args.length != 2) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function is_outside_range takes 2 parameters, not %i", args.length));
            }
            Double metricValue = taskStats.getMetric(metric);
            if (Objects.isNull(metricValue)) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("Metric %s for TaskStats %s is null", metric.name(), taskStats.getId()));
            }
            Double min = new Double(args[0].toString());
            Double max = new Double(args[1].toString());

            if (min > max) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function is_outside_range: min should be less than max"));
            }

            if (metricValue <= min || max <= metricValue) {
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            } else {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], metric %s = %f is not less than %f or greater than %f", taskStats.getId(), metric.name(), metricValue, min, max));
            }
        }
    }

    static class ConfidenceIntervalSigm extends DataQualityFunction implements Function<Object[], ExpressionEvaluatorResponse> {
        private final TaskTrackerService taskTrackerService;
        private Project project;
        private Task task;
        private TaskStats taskStats;
        private DataQualityCondition.Metric metric;

        public ConfidenceIntervalSigm(TaskTrackerService taskTrackerService) {
            this.taskTrackerService = taskTrackerService;

            setName("confidence_interval_sigma");
            setArgsNum(4);
            setDescription("Evaluation of confidence interval with 3 sigma rule");
            setArguments(Lists.newArrayList(
                    new DataQualityFunctionArgument(
                            "center function",
                            ArgumentType.STRING,
                            Arrays.asList("'mean'", "'median'"),
                            "'mean'",
                            "Function for measuring center"),
                    new DataQualityFunctionArgument(
                            "k",
                            ArgumentType.NUMBER,
                            Arrays.asList("1", "2", "3"),
                            "1",
                            "Sigma coefficient"),
                    new DataQualityFunctionArgument(
                            "stats number",
                            ArgumentType.NUMBER,
                            Lists.newArrayList(),
                            "20",
                            "Statistics number used to calculate confidence interval"),
                    new DataQualityFunctionArgument(
                            "delta",
                            ArgumentType.BOOLEAN,
                            Lists.newArrayList("true", "false"),
                            "false",
                            "Delta calculation indicator"))
            );
        }

        @Override
        public void setProject(Project project) {
            this.project = project;
        }

        @Override
        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void setTaskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
        }

        @Override
        public void setMetric(DataQualityCondition.Metric metric) {
            this.metric = metric;
        }

        @Override
        public ExpressionEvaluatorResponse apply(Object[] args) {
            if (args.length != 4) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function confidence_interval_sigma takes only 4 parameters, not %i", args.length));
            }
            Double currentMetricValue = taskStats.getMetric(metric);
//            TODO какой warning
            if (Objects.isNull(currentMetricValue)) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("Metric %s for TaskStats %s is null", metric.name(), taskStats.getId()));
            }

            String functionName = args[0].toString();
            Integer k = Integer.parseInt(args[1].toString());
            Integer statsNumber = Integer.parseInt(args[2].toString());
            Boolean calculateDelta = Boolean.valueOf(args[3].toString());

            if (Objects.isNull(functionName)) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function confidence_interval_sigma 1st parameter should be 'mean' or 'median', not %s", functionName));
            }

            if (!functionName.equals("mean") && !functionName.equals("median")) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function confidence_interval_sigma 1st parameter should be 'mean' or 'median', not %s", functionName));
            }
            if (k < 1 || k > 3) {
                throw new ApiException.InvalidDataQualityConditionException(String.format("Function confidence_interval_sigma 2nd parameter should be {1, 2, 3}, not %d", k));
            }

            return calculateDelta ? applyWithDelta(currentMetricValue, functionName, k, statsNumber) : applyWithoutDelta(currentMetricValue, functionName, k, statsNumber);
        }

        private ExpressionEvaluatorResponse applyWithoutDelta(
                final Double validatingValue,
                final String functionName,
                final Integer k,
                final Integer statsNumber
        ) {
            DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
            List<Double> metricValues = taskTrackerService.findLastNSucceededTasksStats(project, task, taskStats.getColumn(), statsNumber)
                    .stream()
                    .map(stat -> stat.getMetric(metric))
                    .filter(Objects::nonNull)
                    .peek(v -> descriptiveStatistics.addValue(v))
                    .collect(Collectors.toList());

            if (metricValues.isEmpty() || metricValues.size() <= 1) {
                System.out.println("TaskStats list is empty or don't have enough metrics");
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            }

            Double centralFuncValue = centralFunctionValue(functionName, descriptiveStatistics);
            Double stdDev = standardDeviation(descriptiveStatistics);

            Double from = centralFuncValue - k * stdDev;
            Double to = centralFuncValue + k * stdDev;

            printDetails(validatingValue, metricValues, centralFuncValue, stdDev, k, from, to);

            if (validatingValue < from || to < validatingValue) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], %s.%s = %f, interval = [%f ; %f]", taskStats.getId(), taskStats.getColumn(), metric.name(), validatingValue, from, to));
            }
            return new ExpressionEvaluatorResponse(Boolean.TRUE, "");

        }

        private void printDetails(
                final Double validatingValue,
                final List<Double> previousMetricValues,
                final Double centralFuncValue,
                final Double standardDeviation,
                final Integer k,
                final Double from,
                final Double to
        ) {
            System.out.println("validatingValue = [" + validatingValue + "], previousMetricValues = [" + previousMetricValues + "], centralFuncValue = [" + centralFuncValue + "], standardDeviation = [" + standardDeviation + "], k = [" + k + "], from = [" + from + "], to = [" + to + "]");
        }

        private ExpressionEvaluatorResponse applyWithDelta(
                final Double validatingValue,
                final String functionName,
                final Integer k,
                final Integer statsNumber
        ) {
            DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();

//            Порядок нужен для того, чтобы получить корректные значения дельт
            List<Double> metricValues = taskTrackerService.findLastNSucceededTasksStats(project, task, taskStats.getColumn(), statsNumber + 1)
                    .stream()
                    .sorted(Comparator.comparing(TaskStats::getTask, Comparator.comparing(Task::getNominalDate).reversed()))
                    .map(stat -> stat.getMetric(metric))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (metricValues.isEmpty() || metricValues.size() <= 2) {
                System.out.println("TaskStats list is empty or don't have enough metrics");
                return new ExpressionEvaluatorResponse(Boolean.TRUE, "");
            }

            Double validatingDeltaValue = validatingValue - metricValues.get(0);
            List<Double> deltaMetricValues = Lists.newArrayList();
            for (int i = 1; i < metricValues.size(); i++) {
                double deltaValue = metricValues.get(i - 1) - metricValues.get(i);
                deltaMetricValues.add(deltaValue);
                descriptiveStatistics.addValue(deltaValue);
            }

            Double centralFuncValue = centralFunctionValue(functionName, descriptiveStatistics);
            Double stdDev = standardDeviation(descriptiveStatistics);

            Double from = centralFuncValue - k * stdDev;
            Double to = centralFuncValue + k * stdDev;

            printDetails(validatingDeltaValue, deltaMetricValues, centralFuncValue, stdDev, k, from, to);

            if (validatingDeltaValue < from || to < validatingDeltaValue) {
                return new ExpressionEvaluatorResponse(Boolean.FALSE, String.format("TaskStats [%d], %s.%s = %f, delta = %f, interval = [%f ; %f]", taskStats.getId(), taskStats.getColumn(), metric.name(), validatingValue, validatingDeltaValue, from, to));
            }
            return new ExpressionEvaluatorResponse(Boolean.TRUE, "");

        }

        private Double centralFunctionValue(final String functionName, final DescriptiveStatistics descriptiveStatistics) {
            return functionName.equals("mean")
                    ? descriptiveStatistics.getMean()
                    : descriptiveStatistics.getPercentile(50);
        }

        private Double standardDeviation(final DescriptiveStatistics descriptiveStatistics) {
            return sqrt(descriptiveStatistics.getPopulationVariance());
        }

    }

    public static class ExpressionEvaluatorResponse {
        private Boolean isFullfilled;
        private String message;

        public ExpressionEvaluatorResponse(Boolean isFullfilled, String message) {
            this.isFullfilled = isFullfilled;
            this.message = message;
        }

        public Boolean isFullfilled() {
            return isFullfilled;
        }

        public String getMessage() {
            return message;
        }
    }
}
