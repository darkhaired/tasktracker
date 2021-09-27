package tasktracker.backend.controller;

import org.springframework.stereotype.Component;
import tasktracker.backend.controller.model.WarningModel;
import tasktracker.backend.model.Warning;

@Component
public class WarningMapper {

    public WarningModel to(final Warning warning) {
        final WarningModel model = new WarningModel();
        model.setMessage(warning.getMessage());

        return model;
    }

}
