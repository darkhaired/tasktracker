package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HeartbeatModel {
    @JsonProperty("uptime")
    private Long uptime;
    @JsonProperty("version")
    private String version;
}
