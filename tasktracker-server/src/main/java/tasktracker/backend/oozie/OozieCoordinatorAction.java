package tasktracker.backend.oozie;

public class OozieCoordinatorAction {
    private String id;
    private String jobId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        return "OozieCoordinatorAction{" +
                "id='" + id + '\'' +
                ", jobId='" + jobId + '\'' +
                '}';
    }
}
