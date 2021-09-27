package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import tasktracker.backend.controller.model.HeartbeatModel;
import tasktracker.backend.service.InfoService;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api")
public class MainController {
    private final InfoService infoService;

    @RequestMapping(path = "/heartbeat", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public HeartbeatModel heartbeat() {
        return new HeartbeatModel(infoService.uptime(), infoService.version());
    }


}
