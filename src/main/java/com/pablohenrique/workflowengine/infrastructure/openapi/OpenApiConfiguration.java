package com.pablohenrique.workflowengine.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    OpenAPI workflowEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Workflow Engine API")
                        .version("v1")
                        .description("Modelagem, execução e auditoria de workflows corporativos."))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
