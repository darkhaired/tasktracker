package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"id"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskError {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("type")
    private ErrorType type;
    @JsonProperty("task_id")
    private Long task_Id;

    public static TaskError of() {
        return new TaskError();
    }

    public enum ErrorType {

        DATA_LOAD_ERROR("DATA_LOAD_ERROR"),
        NO_DATA_ERROR("NO_DATA_ERROR"),
        MODEL_NOT_FOUND_ERROR("MODEL_NOT_FOUND_ERROR"),
        RUNTIME_ERROR("RUNTIME_ERROR");

        private final String value;

        ErrorType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

}
