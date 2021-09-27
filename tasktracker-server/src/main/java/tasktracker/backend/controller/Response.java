package tasktracker.backend.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<R> {
    @JsonProperty("success")
    private final boolean success;
    @JsonProperty("response")
    private final R response;
    @JsonProperty("errors")
    private final List<Error> errors;

    private Response(final R response) {
        this.success = true;
        this.response = response;
        this.errors = null;
    }

    private Response(final List<Error> errors) {
        this.response = null;
        this.errors = errors;
        this.success = false;
    }

    public static <R> Response<R> success(final R value) {
        return new Response<>(value);
    }

    public static <R> Response<R> fail(final Error... error) {
        return new Response<>(Arrays.asList(error));
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", response=" + response +
                ", errors=" + errors +
                '}';
    }

    @AllArgsConstructor
    @ToString
    public static class Error {
        @JsonProperty("code")
        private final int code;
        @JsonProperty("message")
        private final String message;
        @JsonProperty
        private final String description;
    }
}
