package com.pablohenrique.workflowengine.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Hospedagem do console web (SPA, ADR-008). O build fica em {@code classpath:/static/} dentro do jar; a
 * propriedade {@code workflow-engine.web.console-location} permite apontar para outro local, como
 * {@code file:frontend/dist/} durante os testes end-to-end.
 *
 * <p>Os arquivos de {@code /assets} têm hash no nome, então podem ficar em cache por um ano; o
 * {@code index.html} nunca é cacheado, para que uma nova versão chegue ao navegador imediatamente.
 */
@Configuration
class WebConsoleConfiguration implements WebMvcConfigurer {

    private final String consoleLocation;

    WebConsoleConfiguration(@Value("${workflow-engine.web.console-location:classpath:/static/}") String consoleLocation) {
        this.consoleLocation = consoleLocation.endsWith("/") ? consoleLocation : consoleLocation + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(consoleLocation + "assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registry.addResourceHandler("/favicon.svg")
                .addResourceLocations(consoleLocation)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());
    }
}
