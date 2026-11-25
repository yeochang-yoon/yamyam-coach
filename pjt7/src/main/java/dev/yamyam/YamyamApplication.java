package dev.yamyam;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("dev.yamyam.repository")
@OpenAPIDefinition(info = @Info(title = "냠냠코치 API", version = "1.0"))
public class YamyamApplication {

    public static void main(String[] args) {
        SpringApplication.run(YamyamApplication.class, args);
    }
}
