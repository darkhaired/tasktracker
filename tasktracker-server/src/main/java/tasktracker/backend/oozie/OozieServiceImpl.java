package tasktracker.backend.oozie;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OozieServiceImpl implements OozieService {
    private final RowMapper<OozieWorkflowJob> oozieWorkflowJobMapper = (resultSet, i) -> {
        final String id = resultSet.getString("id");
        final String appName = resultSet.getString("app_name");
        final String appPath = resultSet.getString("app_path");
        final String conf = resultSet.getString("conf");
        final Date createdTime = resultSet.getDate("created_time");
        final Date endTime = resultSet.getDate("end_time");
        final String externalId = resultSet.getString("external_id");
        final String groupName = resultSet.getString("group_name");
        final Date lastModifiedTime = resultSet.getDate("last_modified_time");
        final String logToken = resultSet.getString("log_token");
        final String parentId = resultSet.getString("parent_id");
        final String protoActionConf = resultSet.getString("proto_action_conf");
        final Integer run = resultSet.getInt("run");
        final Date startTime = resultSet.getDate("start_time");
        final String status = resultSet.getString("status");
        final String userName = resultSet.getString("user_name");

        final OozieWorkflowJob job = new OozieWorkflowJob();
        job.setId(id);
        job.setAppName(appName);
        job.setAppPath(appPath);
        job.setConf(conf);
        job.setCreatedTime(createdTime);
        job.setEndTime(endTime);
        job.setExternalId(externalId);
        job.setGroupName(groupName);
        job.setLastModifiedTime(lastModifiedTime);
        job.setLogToken(logToken);
        job.setParentId(parentId);
        job.setProtoActionConf(protoActionConf);
        job.setRun(run);
        job.setStartTime(startTime);
        job.setStatus(status);
        job.setUserName(userName);

        return job;
    };
    private final JdbcTemplate jdbcTemplate;
    private RowMapper<OozieCoordinatorAction> oozieCoordinatorActionMapper = (resultSet, i) -> {
        final String id = resultSet.getString("id");
        final String jobId = resultSet.getString("job_id");

        final OozieCoordinatorAction action = new OozieCoordinatorAction();
        action.setId(id);
        action.setJobId(jobId);

        return action;
    };
    private RowMapper<OozieCoordinatorJob> oozieCoordinatorJobMapper = (resultSet, i) -> {
        final String id = resultSet.getString("id");
        final String appName = resultSet.getString("app_name");
        final Date nextMatdTime = resultSet.getDate("next_matd_time");

        final OozieCoordinatorJob job = new OozieCoordinatorJob();
        job.setId(id);
        job.setAppName(appName);
        job.setNextMatdTime(nextMatdTime);

        return job;
    };

    @Autowired
    public OozieServiceImpl(
            @Value("${oozie.datasource.username:oozie-viewer}") final String userName,
            @Value("${oozie.datasource.password:test}") final String password,
            @Value("${oozie.datasource.url:jdbc:mysql://test/oozie}") final String jdbcUrl,
            @Value("${oozie.datasource.driver-class-name:com.mysql.jdbc.Driver}") final String driverClassName
    ) {
        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(userName);
        config.setPassword(password);
        config.addDataSourceProperty("serverTimezone", "UTC");

        this.jdbcTemplate = new JdbcTemplate(new HikariDataSource(config));
    }

    @Override
    public Optional<OozieWorkflowJob> findWorkflowJob(final String id) throws OozieServiceException {
        try {
            final List<OozieWorkflowJob> jobs = jdbcTemplate.query(
                    "SELECT * FROM WF_JOBS WHERE id = ?",
                    oozieWorkflowJobMapper,
                    id
            );
            return jobs.isEmpty() ? Optional.empty() : Optional.of(jobs.get(0));
        } catch (Exception e) {
            throw new OozieServiceException("Workflow job id ='" + id + '"', e);
        }
    }

    @Override
    public Optional<OozieCoordinatorJob> findCoordinatorJob(OozieWorkflowJob job) throws OozieServiceException {
        try {
            final List<OozieCoordinatorJob> coordinatorJobs = jdbcTemplate.query(
                    "SELECT T2.* FROM COORD_ACTIONS T1 JOIN COORD_JOBS T2 ON T1.job_id=T2.id WHERE T1.id = ? LIMIT 1",
                    oozieCoordinatorJobMapper,
                    job.getParentId()
            );
            return coordinatorJobs.isEmpty() ? Optional.empty() : Optional.of(coordinatorJobs.get(0));
        } catch (Exception e) {
            throw new OozieServiceException("Workflow job ='" + job + '"', e);
        }
    }
}
