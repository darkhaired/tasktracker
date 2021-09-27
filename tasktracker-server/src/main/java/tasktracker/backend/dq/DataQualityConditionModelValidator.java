package tasktracker.backend.dq;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import tasktracker.backend.controller.model.DataQualityConditionModel;
import tasktracker.backend.model.DataQualityCondition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DataQualityConditionModelValidator implements Validator {
    private final ExpressionsEvaluator expressionsEvaluator;

    public static String extractFunctionName(final String expression) {
        return expression.substring(0, expression.indexOf("("));
    }

    public static List<String> extractArguments(final String expression) {
        return Arrays.asList(expression.substring(expression.indexOf("(") + 1, expression.indexOf(")"))
                .split(","));
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return DataQualityConditionModel.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DataQualityConditionModel model = (DataQualityConditionModel) target;

        List<String> existingMetrics = Arrays.asList(DataQualityCondition.Metric.values())
                .stream()
                .map(m -> m.name())
                .collect(Collectors.toList());

        String metric = model.getMetric();
        if (!existingMetrics.contains(metric)) {
            errors.reject(String.format("Metric %s does not exist", metric));
        }

        String expression = model.getExpression().replaceAll("\\s+", "");
        try {
            expressionsEvaluator.tryParse(expression);
        } catch (Exception e) {
            errors.reject(e.getMessage());
            return;
        }

        List<DataQualityFunction> existingFunctions = expressionsEvaluator.getFunctions();
        String functionName = extractFunctionName(expression);
        DataQualityFunction function = existingFunctions.stream()
                .filter(dataQualityFunction -> dataQualityFunction.getName().equals(functionName))
                .findFirst().orElse(null);
        if (Objects.isNull(function)) {
            errors.reject(String.format("Function %s does not exist", functionName));
            return;
        }

        List<String> params = extractArguments(expression);
        if (params.size() != function.getArgsNum()) {
            errors.reject(String.format("Function %s takes %d arguments", functionName, function.getArgsNum()));
            return;
        }

        List<DataQualityFunction.DataQualityFunctionArgument> args = function.getArguments();
        for (int i = 0; i < args.size(); i++) {
            String param = params.get(i);
            DataQualityFunction.DataQualityFunctionArgument arg = args.get(i);

            if (!arg.getFixedValues().isEmpty()) {
                if (!arg.getFixedValues().contains(param)) {
                    errors.reject(String.format("%d argument of function %s can only take these values %s", i + 1, functionName, arg.getFixedValues().toString()));
                }
            } else {
                if (arg.getArgumentType() == DataQualityFunction.ArgumentType.NUMBER) {
                    try {
                        Integer.parseInt(param);
                    } catch (NumberFormatException e) {
                        errors.reject(String.format("%d argument of function %s should be of type number", i, functionName));
                    }
                } else if (arg.getArgumentType() == DataQualityFunction.ArgumentType.STRING) {
                    if (param.matches(".*\\d.*")) {
                        errors.reject(String.format("%d argument of function %s should be of type string", i, functionName));
                    }
                }
            }
        }

    }
}
