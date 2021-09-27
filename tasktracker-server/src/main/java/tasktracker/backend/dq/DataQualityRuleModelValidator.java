package tasktracker.backend.dq;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import tasktracker.backend.controller.model.DataQualityRuleModel;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DataQualityRuleModelValidator implements Validator {
    private final DataQualityConditionModelValidator conditionModelValidator;

    public static void main(String[] args) {
        String tmp = "isbelow(5, )".replaceAll("\\s+", "");
        List<String> params = Arrays.asList(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).split(","));
        System.out.println("params = " + params);
        System.out.println("params.size() = " + params.size());
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return DataQualityRuleModel.class.equals(aClass);
    }


    @Override
    public void validate(Object o, Errors errors) {
        DataQualityRuleModel model = (DataQualityRuleModel) o;

        model.getConditions().forEach(c -> {
            conditionModelValidator.validate(c, errors);
        });
    }

}
