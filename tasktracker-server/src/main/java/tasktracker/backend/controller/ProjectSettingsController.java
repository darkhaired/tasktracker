package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.model.ProjectSettingsModel;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.ProjectSettings;
import tasktracker.backend.service.TaskTrackerService;

import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1")
public class ProjectSettingsController {

    private final TaskTrackerService taskTrackerService;

    @GetMapping("/projects/{id}/settings")
    public Response<ProjectSettingsModel> getSettings(@PathVariable(name = "id") final Long id) {
        final Project project = taskTrackerService.findProjectById(id).orElse(null);
        if (Objects.isNull(project)) {
            return Response.fail(new Response.Error(666, "NOT FOUND", "project"));
        }

        final ProjectSettings settings = project.getSettings();
        if (Objects.isNull(settings)) {
            return Response.fail(new Response.Error(666, "NOT FOUND", "settings"));
        }

        return Response.success(new ProjectSettingsModel(settings));
    }

    @PostMapping("/projects/{id}/settings")
    public Response<ProjectSettingsModel> save(@PathVariable(name = "id") final Long id, @RequestBody final ProjectSettingsModel model) {
        final Project project = taskTrackerService.findProjectById(id).orElse(null);
        if (Objects.isNull(project)) {
            return Response.fail(new Response.Error(666, "NOT FOUND", "project"));
        }

        if (Objects.isNull(model)) {
            return Response.fail(new Response.Error(666, "BAD PARAM", "settings"));
        }

        return Response.success(new ProjectSettingsModel(taskTrackerService.saveProjectSettings(project, model.to())));
    }

}
