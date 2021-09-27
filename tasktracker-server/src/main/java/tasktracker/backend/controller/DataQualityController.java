package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.model.DataQualityOptions;
import tasktracker.backend.controller.model.DataQualityRuleModel;
import tasktracker.backend.controller.model.TableModel;
import tasktracker.backend.controller.model.WarningModel;
import tasktracker.backend.controller.model.dataqualityfrontend.FrontendDQRule;
import tasktracker.backend.dq.DataQualityRuleModelValidator;
import tasktracker.backend.dq.ExpressionsEvaluator;
import tasktracker.backend.model.DataQualityRule;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.Warning;
import tasktracker.backend.service.RuleService;
import tasktracker.backend.service.TaskTrackerService;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateStringOrNull;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2")
public class DataQualityController {
    private final TaskTrackerService taskTrackerService;
    private final RuleService ruleService;
    private final DataQualityRuleModelValidator ruleValidator;
    private final ExpressionsEvaluator expressionsEvaluator;

    @GetMapping(path = "/projects/{project_id}/dataquality/options")
    public ResponseEntity<?> getTablesWithColumns(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "task_name", required = false) final String taskName
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        List<TableModel> tableModels = taskTrackerService.tableToColumn(project, taskName);
        List<DataQualityOptions.DataQualityFunctionModel> functionModels = expressionsEvaluator.getFunctions()
                .stream()
                .map(DataQualityOptions.DataQualityFunctionModel::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new DataQualityOptions(functionModels, tableModels));
    }

    @PostMapping("/projects/{project_id}/dataquality/rules")
    public ResponseEntity<?> createRule(
            @PathVariable(name = "project_id") final Long projectId,
            @Valid @RequestBody final DataQualityRuleModel ruleModel,
            final BindingResult bindingResult
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();
        if (bindingResult.hasFieldErrors()) {
            for (FieldError e : bindingResult.getFieldErrors()) {
                builder.addError(e.getDefaultMessage());
            }
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        ruleValidator.validate(ruleModel, bindingResult);
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(e -> {
                builder.addError(e.getCode());
            });
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }

        DataQualityRule rule = ruleModel.to();

        return ResponseEntity.ok(new DataQualityRuleModel(ruleService.save(project, rule)));
    }

    @GetMapping("/projects/{project_id}/dataquality/rules")
    public ResponseEntity<?> getRules(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "task_name", required = false) final String taskName
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        List<DataQualityRule> projectRules = ruleService.findByProjectAndTaskName(project, taskName);

        return ResponseEntity.ok(projectRules.stream()
                .map(DataQualityRuleModel::new)
                .collect(Collectors.toList()));
    }

    @GetMapping("/projects/{project_id}/dataquality/rules/{rule_id}")
    public ResponseEntity<?> getRule(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "rule_id") final Long ruleId,
            @RequestParam(name = "format_frontend", required = false, defaultValue = "false") final Boolean formatForFrontend
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        DataQualityRule rule = ruleService.findRule(ruleId).orElse(null);
        if (Objects.isNull(rule)) {
            return new ResponseEntity<>(builder.addError(String.format("Rule collection [%d] not found", ruleId)).build(), HttpStatus.NOT_FOUND);
        }

        if (!formatForFrontend)
            return ResponseEntity.ok(new DataQualityRuleModel(rule));
        else {
            FrontendDQRule frontendDQRule = ruleService.collectionToFrontendFormat(rule, expressionsEvaluator.getFunctions());
            return ResponseEntity.ok(frontendDQRule);
        }
    }

    @PutMapping("/projects/{project_id}/dataquality/rules/{rule_id}")
    public ResponseEntity<?> updateRule(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "rule_id") final Long ruleId,
            @Valid @RequestBody final DataQualityRuleModel ruleModel,
            final BindingResult bindingResult
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        DataQualityRule rule = ruleService.findRule(ruleId).orElse(null);
        if (Objects.isNull(rule)) {
            return new ResponseEntity<>(builder.ruleNotFound(ruleId).build(), HttpStatus.NOT_FOUND);
        }
        DataQualityRule updatedRule = ruleModel.to();

        return ResponseEntity.ok(new DataQualityRuleModel(ruleService.save(project, updatedRule)));
    }

    @DeleteMapping("/projects/{project_id}/dataquality/rules/{rule_id}")
    public ResponseEntity<?> deleteRule(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "rule_id") final Long ruleId
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        DataQualityRule rule = ruleService.findRule(ruleId).orElse(null);
        if (Objects.isNull(rule)) {
            return new ResponseEntity<>(builder.ruleNotFound(ruleId).build(), HttpStatus.NOT_FOUND);
        }

        ruleService.delete(rule);

        return ResponseEntity.ok("Ok");
    }

    @GetMapping("/projects/{project_id}/warnings")
    public ResponseEntity<?> getWarnings(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "start_date") final String from,
            @RequestParam(name = "end_date") final String to,
            @RequestParam(name = "task_name", required = false) final String taskName
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            return new ResponseEntity<>(builder.badParam("startDate").build(), HttpStatus.NOT_FOUND);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            return new ResponseEntity<>(builder.badParam("endDate").build(), HttpStatus.NOT_FOUND);
        }

        List<Warning> warnings = taskTrackerService.findWarnings(project, startDate, endDate, taskName);

        return ResponseEntity.ok(warnings.stream()
                .map(WarningModel::new)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/projects/{project_id}/warnings")
    public ResponseEntity<?> deleteWarnings(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate", required = false) final String from,
            @RequestParam(name = "endDate", required = false) final String to,
            @RequestParam(name = "taskName", required = false) final String taskName,
            @RequestParam(name = "taskId", required = false) final Long taskId,
            @RequestParam(name = "resetAnalyzedFlag", required = false, defaultValue = "false") final Boolean resetAnalyzedFlag
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        if (Objects.nonNull(taskId)) {
            final Task task = taskTrackerService.findTaskById(taskId).orElse(null);
            if (Objects.isNull(task)) {
                return new ResponseEntity<>(builder.taskNotFound(taskId).build(), HttpStatus.NOT_FOUND);
            }
            taskTrackerService.deleteWarningsAndSetUnanalyzed(task, resetAnalyzedFlag);
            return ResponseEntity.ok("Ok");
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            return new ResponseEntity<>(builder.badParam("startDate").build(), HttpStatus.NOT_FOUND);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            return new ResponseEntity<>(builder.badParam("endDate").build(), HttpStatus.NOT_FOUND);
        }

        taskTrackerService.deleteWarningsAndSetUnanalyzed(project, startDate, endDate, taskName, resetAnalyzedFlag);
        return ResponseEntity.ok("Ok");
    }
}
