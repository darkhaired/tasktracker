package tasktracker.backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(name = "data_quality_condition")
public class DataQualityCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "column_name")
    private String columnName;
    @Enumerated(EnumType.STRING)
    @Column(name = "metric")
    private Metric metric;
    @Column(name = "expression")
    private String expression;
    @ManyToOne
    @JoinColumn(name = "data_quality_rule_id")
    private DataQualityRule dataQualityRule;

    public enum Metric {
        count,
        total_count,
        unique_count,
        mean,
        std_dev,
        min,
        max,
        quantile_5,
        quantile_15,
        quantile_25,
        quantile_50,
        quantile_75,
        quantile_90,
        quantile_95
    }

    @Override
    public String toString() {
        return "DataQualityCondition{" +
                "id=" + id +
                ", dataQualityRule_id=" + dataQualityRule.getId() +
                ", columnName='" + columnName + '\'' +
                ", metric=" + metric +
                ", expression='" + expression + '\'' +
                '}';
    }
}
