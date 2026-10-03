package com.pablohenrique.workflowengine.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrega o {@code index.html} do console web para as rotas da própria SPA, permitindo abrir ou recarregar
 * qualquer tela pelo endereço. Sem o build do front-end disponível, responde 404.
 */
@Controller
class WebConsoleController {

    private final Resource index;

    WebConsoleController(ResourceLoader resourceLoader,
                         @Value("${workflow-engine.web.console-location:classpath:/static/}") String consoleLocation) {
        String location = consoleLocation.endsWith("/") ? consoleLocation : consoleLocation + "/";
        this.index = resourceLoader.getResource(location + "index.html");
    }

    @GetMapping(value = {"/", "/login", "/dashboard", "/workflows", "/workflows/**", "/definitions",
            "/definitions/**", "/audit", "/users", "/users/**", "/profile"}, produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<Resource> index() {
        if (!index.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }
}
