package dev.atul.apigateway;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/cars")
                        .uri("lb://car-service"))
                .build();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }


    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}

@Data
class Car {
    private String name;
    private LocalDate releaseDate;
}

@RestController
class FaveCarsController {

    private final WebClient.Builder carClient;

    public FaveCarsController(WebClient.Builder carClient) {
        this.carClient = carClient;
    }

    @GetMapping("/fave-cars")
    public Flux<Car> faveCars() {
        return carClient.build().get().uri("lb://car-service/cars")
                .retrieve().bodyToFlux(Car.class)
                .filter(this::isFavorite);
    }

    private boolean isFavorite(Car car) {
        return car.getName().equals("ID_CROZZ");
    }
}