package com.wokoworks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author luobing
 */
@Configuration
@Profile("!prod")
public class NoProdConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {

		registry.addMapping("/**")
				.allowCredentials(true)
				.allowedHeaders("*")
				.allowedOrigins("*")
				.allowedMethods("*");

	}

	@EnableSwagger2
	@Configuration
	@Profile(value = {"local","dev", "test", "demo"})
	public static class SwaggerConfig {
		@Bean
		public Docket createRestApi() {

			return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.apis(RequestHandlerSelectors.basePackage("com.wokoworks.controller"))
				.paths(PathSelectors.any())
				.build();
		}

		private ApiInfo apiInfo() {
			return new ApiInfoBuilder()
				.title("ethereum")
				.description("server")
				.version("v1.0")
				.build();
		}
	}
}
