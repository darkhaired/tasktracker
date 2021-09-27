package tasktracker.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class ApplicationConfiguration {

    @Value("${profile:development}")
    private String profile = "development";

    public String getProfile() {
        return profile;
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(getProfile());
    }

}
