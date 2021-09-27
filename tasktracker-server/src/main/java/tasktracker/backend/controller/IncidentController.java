package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tasktracker.backend.controller.model.IncidentModel;
import tasktracker.backend.model.Project;
import tasktracker.backend.service.IncidentService;
import tasktracker.backend.service.TaskTrackerService;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/")
public class IncidentController {
    private final TaskTrackerService taskTrackerService;
    private final IncidentService incidentService;

    @GetMapping("/incidents")
    public Response<List<IncidentModel>> find(
            @RequestParam(value = "project_id") final Long projectId,
            @RequestParam(value = "date") final String dateString
    ) {
        if (Objects.isNull(projectId)) {
            return Response.fail(new Response.Error(666, "MISSING PARAM", "project_id"));
        }

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return Response.fail(new Response.Error(666, "NOT FOUND", "project"));
        }

        if (Objects.isNull(dateString)) {
            return Response.fail(new Response.Error(666, "MISSING PARAM", "date"));
        }

        final Date date = DateTimePatterns.getDateFromFormattedDateStringOrNull(dateString);
        if (Objects.isNull(date)) {
            return Response.fail(new Response.Error(666, "BAD PARAM", "date"));
        }

        return Response.success(incidentService.findIncidents(project, date)
                .stream()
                .filter(incident -> incident.getTask().getProject().equals(project))
                .map(IncidentModel::new)
                .collect(Collectors.toList()));
    }

}
