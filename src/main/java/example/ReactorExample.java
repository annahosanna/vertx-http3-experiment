package example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorExample {
    public static void main(String[] args) {
        // Creating a Flux (0..n elements)
        Flux<Integer> numbers = Flux.just(1, 2, 3, 4, 5);

        numbers
            .map(n -> n * 2)
            .filter(n -> n > 5)
            .subscribe(System.out::println);

        // Creating a Mono (0..1 elements) 
        Mono<String> message = Mono.just("Hello Reactor!");

        message
            .map(String::toUpperCase)
            .subscribe(System.out::println);
    }
}