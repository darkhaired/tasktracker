package tasktracker.backend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProject(Project project);

    Optional<Task> findTopByProjectIdAndNameAndStateOrderByIdDesc(Long projectId, String taskName, Task.State state);

    Optional<Task> findTopByProjectIdAndNameOrderByIdDesc(Long projectId, String taskName);

    @Query(value = "SELECT \n" +
            "NAME \n" +
            "FROM TASK_STATE\n" +
            "WHERE \n" +
            "    PROJECT_ID = :PROJECT_ID \n" +
            "GROUP BY \n" +
            "    NAME",
            nativeQuery = true)
    List<String> getTaskNames(@Param("PROJECT_ID") Long projectId);

    List<Task> findByProjectIdAndStartDateGreaterThanEqualAndStartDateLessThan(Long projectId, Date startDateFrom, Date startDateTo);

    List<Task> findByProjectIdAndNominalDateGreaterThanEqualAndNominalDateLessThan(Long projectId, Date nominalDateFrom, Date nominalDateTo);

    @Query(
            "FROM TaskState t WHERE t.project.id = :projectId AND " +
                    "cast(nominalDate as date) >= cast(:start as date) AND " +
                    "cast(nominalDate as date) <= cast(:end as date)"
    )
    List<Task> findTasks(
            @Param("projectId") Long projectId,
            @Param("start") Date nominalDateFrom,
            @Param("end") Date nominalDateTo
    );

    List<Task> findByProjectIdAndEndDateGreaterThanEqualAndEndDateLessThan(Long projectId, Date endDateFrom, Date endDateTo);

    List<Task> findByProjectIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(Long projectId, Date startDate, Date endDate);

    List<Task> findByProjectIdAndStartDateGreaterThan(Long projectId, Date startDate);

    List<Task> findByProjectIdAndStartDateGreaterThanEqual(Long projectId, Date startDate);

    List<Task> findByProjectIdAndStartDateLessThan(Long projectId, Date startDate);

    List<Task> findByProjectIdAndStartDateLessThanEqual(Long projectId, Date startDate);

    List<Task> findByProjectIdAndEndDateGreaterThan(Long projectId, Date endDate);

    List<Task> findByProjectIdAndEndDateGreaterThanEqual(Long projectId, Date endDate);

    List<Task> findByProjectIdAndEndDateLessThan(Long projectId, Date endDate);

    List<Task> findByProjectIdAndEndDateLessThanEqual(Long projectId, Date endDate);

    @Query(value = "from TaskState t where cast(startDate as date) = cast(:date as date) and state = :state")
    List<Task> findByDateAndState(@Param("date") Date date, @Param("state") Task.State state);

    @Query("from TaskState t where " +
            "t.project = :project and " +
            "cast(t.startDate as date) >= cast(:start as date) and " +
            "cast(t.startDate as date) <= cast(:end as date)"
    )
    List<Task> findTasksInRage(
            @Param("project") Project project,
            @Param("start") Date start,
            @Param("end") Date end
    );

    @Query("from TaskState t where " +
            "t.project = :project and " +
            "date_trunc('hour', t.startDate) >= date_trunc('hour', cast(:start as timestamp)) and " +
            "date_trunc('hour', t.startDate) <= date_trunc('hour', cast(:end as timestamp))"
    )
    List<Task> findTasksInRageTruncHour(
                    @Param("project") Project project,
                    @Param("start") Date start,
                    @Param("end") Date end
    );

    @Modifying
    @Query("UPDATE TaskState t SET t.name = :newName WHERE t.name = :originalName")
    int renameTasks(@Param("originalName") String originalName, @Param("newName") String newName);


    @Query("FROM TaskState t WHERE t.project = :project AND t.synch = false")
    List<Task> findUnsynchronizedProjectTasks(@Param("project") Project project, Pageable pageable);

    @Query("FROM TaskState t WHERE t.project = :project AND t.analyzed = false AND t.state in (:states)")
    List<Task> findUnanalyzedTasksProjectTasks(@Param("project") Project project, @Param("states") List<Task.State> states, Pageable of);

    @Query(value = "SELECT id , application_id , end_date , name , start_date , status , timestamp , username , project_id  , auto_updated , task_type  , nominal_date , next_date , oozie_workflow_id , oozie_workflow_name , synchronized , analyzed , oozie_coordinator_id " +
            "FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY name ORDER BY id DESC) as rn FROM task_state WHERE project_id = :PROJECT_ID AND status = 'SUCCEEDED') t " +
            "WHERE rn = 1;",
    nativeQuery = true)
    List<Task> findDistinctLatestSucceededTasksByProject(@Param("PROJECT_ID") Long projectId);


//    @Query(nativeQuery = true,
//            value = "FROM TaskState t " +
//                    "WHERE t.project = :project AND " +
//                    "t.state = :state AND " +
//                    "cast(nominalDate as date) < cast(:end as date) " +
//                    "ORDER BY nominalDate ASC "
//    )
    @Query(nativeQuery = true,
            value = "SELECT * FROM task_state t " +
                    "WHERE project_id = :projectId AND " +
                    "name = :taskName AND " +
                    "status = :state AND " +
                    "cast(nominal_date as date) < cast(:end as date) " +
                    "ORDER BY nominal_date DESC " +
                    "LIMIT :n"
    )
    List<Task> findLastNTasksByProjectAndNominalDateAndState(
            @Param("projectId") Long projectId,
            @Param("taskName") String taskName,
            @Param("state") String state,
            @Param("end") Date nominalDateTo,
            @Param("n") Integer n
    );
}
