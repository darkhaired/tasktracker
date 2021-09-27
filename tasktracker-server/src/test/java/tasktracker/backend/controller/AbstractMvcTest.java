package tasktracker.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest
public abstract class AbstractMvcTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected MockMvc mvc() {
        return this.mvc;
    }

    protected <R> String toJson(final R instance) throws JsonProcessingException {
        return objectMapper.writeValueAsString(instance);
    }

    protected <R> R fromJson(final String json, final Class<R> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
