package com.pablohenrique.workflowengine.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Limita o tamanho do corpo das requisições (padrão 256 KB): um usuário autenticado não consegue inflar o
 * banco nem a memória com variáveis gigantes. Corpos declarados maiores são recusados antes da leitura;
 * corpos sem tamanho declarado são interrompidos ao ultrapassar o limite.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    RequestSizeLimitFilter(@Value("${workflow-engine.http.max-request-size:256KB}") DataSize maxRequestSize) {
        this.maxBytes = maxRequestSize.toBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            reject(request, response);
            return;
        }
        chain.doFilter(new LimitedRequest(request, maxBytes), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"title\":\"Payload too large\",\"status\":413,\"detail\":\"The request body "
                + "exceeds the limit of " + maxBytes + " bytes\",\"instance\":\""
                + request.getRequestURI().replace("\"", "") + "\"}");
    }

    /** Sinaliza que o corpo excedeu o limite durante a leitura. */
    static final class PayloadTooLargeException extends IOException {

        PayloadTooLargeException(long maxBytes) {
            super("Request body exceeds the limit of " + maxBytes + " bytes");
        }
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long maxBytes;

        LimitedRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream delegate = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                @Override
                public int read() throws IOException {
                    int value = delegate.read();
                    if (value != -1) {
                        count(1);
                    }
                    return value;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    int count = delegate.read(buffer, offset, length);
                    if (count > 0) {
                        count(count);
                    }
                    return count;
                }

                private void count(long bytes) throws IOException {
                    read += bytes;
                    if (read > maxBytes) {
                        throw new PayloadTooLargeException(maxBytes);
                    }
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    delegate.setReadListener(listener);
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
