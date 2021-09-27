package tasktracker.backend.oozie;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class OozieWorkflowJob {
    private String id;
    private String appName;
    private String appPath;
    private String conf;
    private Date createdTime;
    private Date endTime;
    private String externalId;
    private String groupName;
    private Date lastModifiedTime;
    private String logToken;
    private String parentId;
    private String protoActionConf;
    private Integer run;
    private Date startTime;
    private String status;
    private String userName;
    private byte[] wfInstance;
}
