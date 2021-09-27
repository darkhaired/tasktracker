package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(name = "task_statistics", indexes = {
        @Index(name = "idx_statistics_task_id", columnList = "task_state_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "unq_tid_column_name", columnNames = {"task_state_id", "column_name"})
})
public class TaskStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "task_state_id")
    private Task task;
    @Column(name = "column_name", nullable = false)
    private String column;
    @Enumerated(EnumType.STRING)
    @Column(name = "column_type", nullable = false, columnDefinition = "character varying(255) default 'STRING' ")
    private ColumnType columnType = ColumnType.STRING;
    @Column(name = "mean")
    private Double mean;
    @Column(name = "std_dev")
    private Double stdDev;
    @Column(name = "min")
    private Double min;
    @Column(name = "max")
    private Double max;
    @Column(name = "total_count")
    private Long totalCount;
    @Column(name = "unique_count")
    private Long uniqueCount;
    @Column(name = "count")
    private Long count;
    @Column(name = "quantile_5")
    private Double quantile5;
    @Column(name = "quantile_15")
    private Double quantile15;
    @Column(name = "quantile_25")
    private Double quantile25;
    @Column(name = "quantile_50")
    private Double quantile50;
    @Column(name = "quantile_75")
    private Double quantile75;
    @Column(name = "quantile_90")
    private Double quantile90;
    @Column(name = "quantile_95")
    private Double quantile95;

    public Double getMetric(DataQualityCondition.Metric metric) {
        switch (metric) {
            case max:
                return max;
            case min:
                return min;
            case total_count:
                return Objects.nonNull(totalCount) ? (double) totalCount : null;
            case count:
                return Objects.nonNull(count) ? (double) count : null;
            case unique_count:
                return Objects.nonNull(uniqueCount) ? (double) uniqueCount : null;
            case std_dev:
                return stdDev;
            case mean:
                return mean;
            case quantile_5:
                return quantile5;
            case quantile_15:
                return quantile15;
            case quantile_25:
                return quantile25;
            case quantile_50:
                return quantile50;
            case quantile_75:
                return quantile75;
            case quantile_90:
                return quantile90;
            case quantile_95:
                return quantile95;
            default:
                return 0.0;
        }
    }


    public enum ColumnType {
        STRING,
        NUMERIC,
        OBJECT
    }
}
