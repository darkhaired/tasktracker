package tasktracker.backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DailyTasks {
    private Map<String, Map<String, List<Task>>> dateToTasks;
}
