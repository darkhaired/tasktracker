package tasktracker.backend.controller.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ChangeTasksNameBody {
    @JsonProperty("original_name")
    private String originalName;
    @JsonProperty("new_name")
    private String newName;
}
