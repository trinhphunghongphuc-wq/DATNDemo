package com.service.impl;

import com.exception.GlobalExceptionHandler;
import com.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SseExceptionHandlingTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @RestController
    static class ErrorController {
        @GetMapping("/sse-disconnect")
        void disconnect(HttpServletResponse response) throws IOException {
            response.setContentType("text/event-stream");
            throw new AsyncRequestNotUsableException("Client disconnected");
        }

        @GetMapping("/sse-timeout")
        void timeout(HttpServletResponse response) {
            response.setContentType("text/event-stream");
            throw new AsyncRequestTimeoutException();
        }

        @GetMapping("/sse-raw-io")
        void rawIo(HttpServletResponse response) throws IOException {
            response.setContentType("text/event-stream");
            throw new IOException("An established connection was aborted");
        }

        @GetMapping("/json-error")
        Object jsonError() { throw new IllegalStateException("Test server error"); }

        @GetMapping("/json-not-found")
        Object missing() { throw new ResourceNotFoundException("Missing batch"); }

        @GetMapping("/json-timeout")
        Object jsonTimeout() { throw new AsyncRequestTimeoutException(); }
    }

    @Test
    void disconnectedStreamDoesNotTryToSerializeErrorResponse() throws Exception {
        mvc.perform(get("/sse-disconnect")).andExpect(content().string(""));
    }

    @Test
    void streamTimeoutDoesNotTryToSerializeErrorResponse() throws Exception {
        mvc.perform(get("/sse-timeout")).andExpect(content().string(""));
    }

    @Test
    void rawContainerIoErrorDoesNotTriggerConverterFailure() throws Exception {
        var result = mvc.perform(get("/sse-raw-io"))
                .andExpect(status().isInternalServerError()).andExpect(content().string("")).andReturn();
        assertEquals("text/event-stream", result.getResponse().getContentType());
    }

    @Test
    void ordinaryErrorsStillReturnJson() throws Exception {
        mvc.perform(get("/json-error")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Test server error"));
        mvc.perform(get("/json-not-found")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Missing batch"));
    }

    @Test
    void ordinaryAsyncTimeoutStillHasJsonError() throws Exception {
        mvc.perform(get("/json-timeout")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
