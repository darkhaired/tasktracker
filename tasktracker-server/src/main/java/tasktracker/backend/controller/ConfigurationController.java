package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.model.ConfigurationModel;
import tasktracker.backend.model.Configuration;
import tasktracker.backend.service.ConfigurationService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/v1/")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @GetMapping("/configuration")
    public Response<List<ConfigurationModel>> all() {
        return Response
                .success(configurationService.all()
                        .stream()
                        .map(configuration -> new ConfigurationModel(configuration.getKey(), configuration.getValue()))
                        .collect(Collectors.toList())
                );
    }

    @PostMapping("/configuration")
    public Response<ConfigurationModel> save(@RequestBody final ConfigurationModel model) {
        if (Objects.isNull(model)) {
            return Response.fail(new Response.Error(666, "MISSING PARAM", "model"));
        }
        if (Objects.isNull(model.getKey())) {
            return Response.fail(new Response.Error(666, "MISSING FIELD", "key"));
        }
        if (Objects.isNull(model.getValue())) {
            return Response.fail(new Response.Error(666, "MISSING FIELD", "value"));
        }

        final Configuration configuration = configurationService.save(model.to());
        return Response.success(new ConfigurationModel(configuration.getKey(), configuration.getValue()));
    }

    @PutMapping("/configuration")
    public Response<ConfigurationModel> update(@RequestBody final ConfigurationModel model) {
        if (Objects.isNull(model)) {
            return Response.fail(new Response.Error(666, "MISSING PARAM", "model"));
        }
        if (Objects.isNull(model.getKey())) {
            return Response.fail(new Response.Error(666, "MISSING FIELD", "key"));
        }
        if (Objects.isNull(model.getValue())) {
            return Response.fail(new Response.Error(666, "MISSING FIELD", "value"));
        }

        final Configuration configuration = configurationService.update(model.to());
        return Response.success(new ConfigurationModel(configuration.getKey(), configuration.getValue()));
    }

    @DeleteMapping("/configuration")
    public Response<Boolean> delete(@RequestParam(name = "key") final String key) {
        if (Objects.isNull(key)) {
            return Response.fail(new Response.Error(666, "MISSING PARAM", "key"));
        }

        final Configuration configuration = configurationService.findByKey(key).orElse(null);
        if (Objects.isNull(configuration)) {
            return Response.fail(new Response.Error(666, "NOT FOUND", "configuration"));
        }

        configurationService.delete(configuration);

        return Response.success(true);
    }
}
