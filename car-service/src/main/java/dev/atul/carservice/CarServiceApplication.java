package dev.atul.carservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.UUID;

@SpringBootApplication
@EnableEurekaClient
@Slf4j
public class CarServiceApplication {

    @Bean
    ApplicationRunner mockData(CarRepository repository) {
        Car ID = new Car(UUID.randomUUID(), "ID", LocalDate.of(2019, Month.DECEMBER, 1));
        Car ID_CROZZ = new Car(UUID.randomUUID(), "ID_CROZZ", LocalDate.of(2021, Month.MAY, 1));
        Car ID_VIZZION = new Car(UUID.randomUUID(), "ID_VIZZION", LocalDate.of(2021, Month.DECEMBER, 1));

        Set<Car> cars = Set.of(ID, ID_CROZZ, ID_VIZZION);

        return args -> {
            repository
                    .deleteAll()
                    .thenMany(
                            Flux.just(cars)
                                    .flatMap(repository::saveAll)
                    )
                    .thenMany(repository.findAll())
                    .subscribe(car -> log.info("saving " + car.toString()));
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(CarServiceApplication.class, args);
    }

}

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class Car {
    @Id
    private UUID id;
    private String name;
    private LocalDate releaseDate;

}

interface CarRepository extends ReactiveMongoRepository<Car, UUID> {

}

@RestController
class CarController {

    private final CarRepository carRepository;

    public CarController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @PostMapping("/cars")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Car> addCar(@RequestBody Car car) {
        return carRepository.save(car);
    }

    @GetMapping("/cars")
    public Flux<Car> getCars() {
        return carRepository.findAll();
    }

    @DeleteMapping("/cars/{id}")
    public Mono<ResponseEntity<Void>> deleteCar(@PathVariable("id") UUID id) {
        return carRepository.findById(id)
                .flatMap(car -> carRepository.delete(car)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}