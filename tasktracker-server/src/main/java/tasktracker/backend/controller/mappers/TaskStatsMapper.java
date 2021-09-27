package tasktracker.backend.controller.mappers;

import org.springframework.stereotype.Component;
import tasktracker.backend.controller.body.TaskStatsBody;
import tasktracker.backend.controller.model.TaskStatsCollectionResponse;
import tasktracker.backend.controller.model.TaskStatsResponse;
import tasktracker.backend.model.TaskStats;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskStatsMapper {
    public TaskStatsResponse to(final TaskStats stats) {
        final TaskStatsResponse response = new TaskStatsResponse();
        response.setColumn(stats.getColumn());
        response.setColumnType(stats.getColumnType().name());
        response.setTotalCount(stats.getTotalCount());
        response.setUniqueCount(stats.getUniqueCount());
        response.setCount(stats.getCount());
        response.setStdDev(stats.getStdDev());
        response.setMax(stats.getMax());
        response.setMean(stats.getMean());
        response.setMin(stats.getMin());
        response.setQuantile5(stats.getQuantile5());
        response.setQuantile15(stats.getQuantile15());
        response.setQuantile25(stats.getQuantile25());
        response.setQuantile50(stats.getQuantile50());
        response.setQuantile75(stats.getQuantile75());
        response.setQuantile90(stats.getQuantile90());

        return response;
    }

    public TaskStatsCollectionResponse to(final List<TaskStats> statistics) {
        final TaskStatsCollectionResponse response = new TaskStatsCollectionResponse();
        response.setStats(statistics.stream().map(this::to).collect(Collectors.toList()));

        return response;
    }

    public TaskStats to(final TaskStatsBody body) {
        final TaskStats stats = new TaskStats();
        stats.setColumn(body.getColumn());
        stats.setColumnType(TaskStats.ColumnType.valueOf(body.getColumnType().toUpperCase().trim()));
        stats.setCount(body.getCount());
        stats.setTotalCount(body.getTotalCount());
        stats.setUniqueCount(body.getUniqueCount());
        stats.setStdDev(body.getStdDev());
        stats.setMax(body.getMax());
        stats.setMean(body.getMean());
        stats.setMin(body.getMin());
        stats.setQuantile5(body.getQuantile5());
        stats.setQuantile15(body.getQuantile15());
        stats.setQuantile25(body.getQuantile25());
        stats.setQuantile50(body.getQuantile50());
        stats.setQuantile75(body.getQuantile75());
        stats.setQuantile90(body.getQuantile90());
        stats.setQuantile95(body.getQuantile95());

        return stats;
    }
}
