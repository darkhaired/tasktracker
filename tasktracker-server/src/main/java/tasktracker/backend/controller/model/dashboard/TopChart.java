package tasktracker.backend.controller.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class TopChart {
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("top")
    private int top;
    @JsonProperty("top_elements")
    private List<? extends TopElement> topElements;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode(of = {"name", "counter"})
    public static class TopElement {
        @JsonProperty("name")
        private String name;
        @JsonProperty("counter")
        private int counter;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode(of = {"taskName"})
    public static class TopWarningElement extends TopElement {
        @JsonProperty("task_name")
        private String taskName;

        public TopWarningElement(String name, int counter, String taskName) {
            super(name, counter);
            this.taskName = taskName;
        }
    }
}
