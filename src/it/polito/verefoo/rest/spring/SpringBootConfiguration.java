package it.polito.verefoo.rest.spring;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

// TODO #jalol separate config from rest api
@SpringBootApplication
@EnableSwagger2
public class SpringBootConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootConfiguration.class, args); 
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            //System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
         

        };
    }

    @Bean
    public HttpMessageConverters converters() {
    	 MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    	   // Note: here we are making this converter to process any kind of response, 
    	   // not only application/*json, which is the default behaviour
    	   converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));        
        return new HttpMessageConverters(true, Arrays.asList(
        		converter,new StringHttpMessageConverter(),
                new Jaxb2RootElementHttpMessageConverter())
        );
    }
    
    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("it.polito.verefoo.rest.spring"))
                .paths(PathSelectors.any())
                .build(); 
    }
}