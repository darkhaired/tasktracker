package tasktracker.backend.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import tasktracker.backend.model.Configuration;

@Getter
@AllArgsConstructor
@ToString
public class ConfigurationModel {
    @JsonProperty("key")
    private String key;
    @JsonProperty("value")
    private String value;

    public Configuration to() {
        final Configuration configuration = new Configuration();
        configuration.setKey(key);
        configuration.setValue(value);

        return configuration;
    }
}
