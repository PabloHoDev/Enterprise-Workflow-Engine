package com.pablohenrique.workflowengine.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestSizeLimitFilterTest {

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(DataSize.ofBytes(10));

    @Test
    void rejectsADeclaredBodyAboveTheLimitWithoutReadingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workflows");
        request.setContent(new byte[11]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"status\":413");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void interruptsAnUndeclaredBodyWhenItCrossesTheLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workflows") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new byte[25]);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        InputStream body = ((HttpServletRequest) chain.getRequest()).getInputStream();
        assertThat(body.read(new byte[10], 0, 10)).isEqualTo(10);
        assertThatThrownBy(body::read).isInstanceOf(IOException.class).hasMessageContaining("exceeds the limit");
    }

    @Test
    void passesBodiesWithinTheLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workflows");
        request.setContent("{\"a\":1}".getBytes());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded.getReader().readLine()).isEqualTo("{\"a\":1}");
    }
}
