package tasktracker.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class RequestLoggingInterceptor extends HandlerInterceptorAdapter {
    private final Logger logger = LoggerFactory.getLogger("duration_logger");
    private ThreadLocal<LocalDateTime> startDate = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startDate.set(LocalDateTime.now());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LocalDateTime now = LocalDateTime.now();
        logger.info("{}   {} {}, time  = {}ms, status = {}",
                now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                request.getMethod(),
                request.getRequestURL().toString(),
                Duration.between(startDate.get(), now).toMillis(),
                response.getStatus());
    }
}
