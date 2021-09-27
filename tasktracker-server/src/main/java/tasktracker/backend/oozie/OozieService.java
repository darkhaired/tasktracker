package tasktracker.backend.oozie;

import java.util.Optional;

public interface OozieService {

    Optional<OozieWorkflowJob> findWorkflowJob(String id) throws OozieServiceException;

    Optional<OozieCoordinatorJob> findCoordinatorJob(OozieWorkflowJob job) throws OozieServiceException;

}
