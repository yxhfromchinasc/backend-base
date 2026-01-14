package com.lvxin.base.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置类
 * 用于生成API文档
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "猫社区后端API文档",
                version = "1.0.0",
                description = "猫社区后端服务API接口文档，包含用户管理、角色权限、快递代取、回收预约等功能",
                contact = @Contact(
                        name = "开发团队",
                        email = "dev@example.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080/api", description = "本地开发环境"),
                @Server(url = "https://bd-miaow.tech/api", description = "生产环境")
        }
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token认证")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"));
    }
}
