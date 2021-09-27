package tasktracker.backend.oozie;

import java.util.Date;

public class OozieCoordinatorJob {
    private String id;
    private String appName;
    private Date nextMatdTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Date getNextMatdTime() {
        return nextMatdTime;
    }

    public void setNextMatdTime(Date nextMatdTime) {
        this.nextMatdTime = nextMatdTime;
    }

    @Override
    public String toString() {
        return "OozieCoordinatorJob{" +
                "id='" + id + '\'' +
                ", appName='" + appName + '\'' +
                ", nextMatdTime=" + nextMatdTime +
                '}';
    }
}
