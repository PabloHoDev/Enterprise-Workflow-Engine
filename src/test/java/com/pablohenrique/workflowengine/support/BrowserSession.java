package com.pablohenrique.workflowengine.support;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Simula o navegador do console web: guarda os cookies recebidos (sessão e XSRF-TOKEN), devolve-os a cada
 * requisição e envia o token CSRF no header, como faz a SPA.
 */
public final class BrowserSession {

    public static final String SESSION_COOKIE = "EWE_SESSION";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";

    private final MockMvc mockMvc;
    private final String remoteAddress;
    private final Map<String, Cookie> cookies = new LinkedHashMap<>();

    public BrowserSession(MockMvc mockMvc, String remoteAddress) {
        this.mockMvc = mockMvc;
        this.remoteAddress = remoteAddress;
    }

    public BrowserSession(MockMvc mockMvc) {
        this(mockMvc, "127.0.0.1");
    }

    /** Busca o token CSRF e tenta o login; devolve a resposta do login. */
    public ResultActions login(String username, String password) throws Exception {
        fetchCsrfToken();
        ResultActions result = perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
        // O token CSRF é trocado a cada login.
        fetchCsrfToken();
        return result;
    }

    public void fetchCsrfToken() throws Exception {
        perform(get("/api/v1/auth/csrf"));
    }

    public ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        ResultActions result = mockMvc.perform(request.with(browser()));
        remember(result.andReturn());
        return result;
    }

    public ResultActions performWithoutCsrf(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(withCookies(false)));
    }

    public boolean hasSession() {
        return cookies.containsKey(SESSION_COOKIE);
    }

    private RequestPostProcessor browser() {
        return withCookies(true);
    }

    private RequestPostProcessor withCookies(boolean sendCsrfHeader) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            request.addHeader("X-Requested-With", "XMLHttpRequest");
            if (!cookies.isEmpty()) {
                request.setCookies(cookies.values().toArray(Cookie[]::new));
            }
            Cookie csrf = cookies.get(CSRF_COOKIE);
            if (sendCsrfHeader && csrf != null) {
                request.addHeader("X-XSRF-TOKEN", csrf.getValue());
            }
            return request;
        };
    }

    private void remember(MvcResult result) {
        MockHttpServletResponse response = result.getResponse();
        for (Cookie cookie : response.getCookies()) {
            if (cookie.getMaxAge() == 0 || cookie.getValue() == null || cookie.getValue().isEmpty()) {
                cookies.remove(cookie.getName());
            } else {
                cookies.put(cookie.getName(), cookie);
            }
        }
    }
}
