package tasktracker.client.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Response<R> {
    @JsonProperty("success")
    @Setter(value = AccessLevel.NONE)
    private final boolean success;
    @JsonProperty("response")
    @Setter(value = AccessLevel.NONE)
    private final R response;
    @JsonProperty("errors")
    @Setter(value = AccessLevel.NONE)
    private final List<Error> errors;
    @JsonProperty("statusCode")
    @Setter(value = AccessLevel.NONE)
    private int statusCode;

    private Response(final int responseCode, final R response) {
        this.success = true;
        this.statusCode = responseCode;
        this.response = response;
        this.errors = null;
    }

    private Response(final int responseCode, final List<Error> errors) {
        this.response = null;
        this.statusCode = responseCode;
        this.errors = errors;
        this.success = false;
    }

    public Response() {
        this.success = false;
        this.statusCode = 0;
        this.response = null;
        this.errors = null;
    }

    private Response(final boolean success, final int responseCode, final R response, final List<Error> errors) {
        this.success = success;
        this.statusCode = responseCode;
        this.response = response;
        this.errors = errors;
    }

    public static <R> Response<R> success(final int responseCode, final R value) {
        return new Response<>(responseCode, value);
    }

    public static <R> Response<R> fail(final int responseCode, final Error... error) {
        return new Response<>(responseCode, Lists.newArrayList(error));
    }

    public static <R> Response<R> fail(final int responseCode, final Error error) {
        return new Response<>(responseCode, Lists.newArrayList(error));
    }

    public R orElse(final R other) {
        if (isSuccess()) {
            return response;
        }

        return other;
    }

    public R get() {
        if (response == null) {
            throw new NoSuchElementException("No value present");
        }
        return response;
    }

    public <T> Response<T> map(final Function<R, T> mapper) {
        if (!isSuccess()) {
            return new Response<>(statusCode, errors);
        }
        return success(statusCode, mapper.apply(get()));
    }

    public void forEach(final Consumer<R> action) {
        if (!isSuccess()) {
            return;
        }
        action.accept(response);
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    @Data
    public static final class Error {
        @JsonProperty("code")
        private final int code;
        @JsonProperty("message")
        private final String message;
        @JsonProperty("description")
        private final String description;

        public Error() {
            this.code = 0;
            this.message = "";
            this.description = "";
        }
    }
}
