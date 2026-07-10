package com.blueforge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Shared MockMvc + real-Postgres plumbing for integration tests that drive several pipeline
// endpoints in sequence and need the actual persisted result back, not just the mocked-repository
// view a unit test would give. @Transactional wraps each test method (and every nested
// @Transactional service call, since default propagation joins the existing transaction) in one
// transaction that's rolled back afterward - there's no Testcontainers here, so without this every
// run would leave permanent rows in whatever Postgres the suite points at.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractProjectApiIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected <T> T postAndRead(String url, Object body, Class<T> type) throws Exception {
        MockHttpServletRequestBuilder request = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        return perform(request, type);
    }

    protected <T> T getAndRead(String url, Class<T> type) throws Exception {
        return perform(get(url), type);
    }

    private <T> T perform(MockHttpServletRequestBuilder request, Class<T> type) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), type);
    }
}
