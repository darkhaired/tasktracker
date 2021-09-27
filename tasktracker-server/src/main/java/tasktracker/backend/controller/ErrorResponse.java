package tasktracker.backend.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ErrorResponse {
    @JsonProperty("errors")
    private List<ErrorModel> errors = Lists.newArrayList();

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    @Data
    @NoArgsConstructor
    public static class ErrorModel {
        @JsonProperty("message")
        private String message;
    }

    public static class ErrorResponseBuilder {
        private List<ErrorModel> errors = Lists.newArrayList();

        public ErrorResponseBuilder addError(final String message) {
            final ErrorModel model = new ErrorModel();
            model.setMessage(message);

            errors.add(model);

            return this;
        }

        public ErrorResponseBuilder projectNotFound(final long projectId) {
            return addError("Project not found by id '" + projectId + "'");
        }

        public ErrorResponseBuilder taskNotFound(final long taskId) {
            return addError("Task not found by id '" + taskId + "'");
        }

        public ErrorResponseBuilder ruleNotFound(final long ruleId) {
            return addError("Rule not found by id '" + ruleId + "'");
        }

        public ErrorResponseBuilder invalidDateFormat(final String value) {
            return addError("Invalid date format '" + value + "'. Expected yyyy-MM-dd");
        }

        public ErrorResponseBuilder badParam(final String value) {
            return addError("Bad param '" + value + "'");
        }

        public ErrorResponseBuilder paramMissing(final String value) {
            return addError("Missing param '" + value + "'");
        }

        public boolean hasError() {
            return !errors.isEmpty();
        }

        public ErrorResponse build() {
            final ErrorResponse response = new ErrorResponse();
            response.setErrors(errors);

            return response;
        }
    }
}
