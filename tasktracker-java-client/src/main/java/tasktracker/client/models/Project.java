package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "name", "description", "createdAt"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    @Accessors(chain = true)
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date createdAt;

    public static Project of() {
        return new Project();
    }

    public static Project create(final String name, final String description) {
        Preconditions.checkArgument(name != null && !name.isEmpty(), "'name' must not be null or empty!");
        Preconditions.checkArgument(description != null && !description.isEmpty(), "'description' must not be null or empty!");

        final Project project = new Project();
        project.setName(name);
        project.setDescription(description);

        return project;
    }

}
