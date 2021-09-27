package tasktracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

@Service
public class InfoService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Date startDatetime = new Date();

    public Long uptime() {
        return System.currentTimeMillis() - startDatetime.getTime();
    }

    public String version() {
        try {
            final InputStream value = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/maven/tasktracker/tasktracker-server/pom.properties");
            final Properties properties = new Properties();
            properties.load(value);
            return properties.getProperty("version");
        } catch (IOException e) {
            logger.error("An error occurred while read pom.properties from JAR file", e);
            return "unknown";
        }
    }
}
