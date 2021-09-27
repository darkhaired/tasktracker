package tasktracker.backend.controller.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class KeyValueModel {
    @JsonProperty("key")
    private String key;
    @JsonProperty("value")
    private Double value;
}
