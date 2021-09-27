package tasktracker.backend.controller.exception;

public abstract class ApiException extends RuntimeException {

    public ApiException() {
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }


    public static class ProjectNotFound extends ApiException {

        public ProjectNotFound(final Long projectId) {
            super(String.format("PROJECT NOT FOUND: [%s]", projectId));
        }

    }

    public static class ProjectAlreadyExists extends ApiException {

        public ProjectAlreadyExists(Long projectId) {
            super(String.format("PROJECT ALREADY EXISTS: [%s]", projectId));
        }

        public ProjectAlreadyExists(String name) {
            super(String.format("PROJECT ALREADY EXISTS: [%s]", name));
        }

    }

    public static class TaskStateNotFound extends ApiException {
        public TaskStateNotFound(final Long taskId) {
            super(String.format("TASK STATE NOT FOUND: [%s]", taskId));
        }
    }

    public static class TaskMetricNotFound extends ApiException {
        public TaskMetricNotFound(final Long metricId) {
            super(String.format("TASK METRIC NOT FOUND: [%s]", metricId));
        }
    }

    public static class TaskErrorNotFound extends ApiException {
        public TaskErrorNotFound(final Long errorId) {
            super(String.format("TASK ERROR NOT FOUND: [%s]", errorId));
        }
    }

    public static class TaskReportNotFound extends ApiException {
        public TaskReportNotFound(final Long errorId) {
            super(String.format("TASK REPORT NOT FOUND: [%s]", errorId));
        }
    }

    public static class InvalidInputData extends  ApiException {
        public InvalidInputData(String message) {
            super(String.format("INVALID INPUT DATA: [%s]", message));
        }
    }

    public static class InvalidDataQualityConditionException extends ApiException {
        public InvalidDataQualityConditionException(String message) {
            super(message);
        }
    }

    public static class InvalidDataQualityRuleException extends ApiException {
        public InvalidDataQualityRuleException(String message) {
            super(message);
        }
    }

    public static class TaskStatsMetricIsNullException extends ApiException {
        public TaskStatsMetricIsNullException(String message) {
            super(message);
        }
    }
}
