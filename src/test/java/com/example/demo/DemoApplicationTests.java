package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoApplicationTests {
    private static final Logger log = LoggerFactory.getLogger(DemoApplicationTests.class);

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void contextLoads() {
        var client = WebClient.create("http://localhost:8080");

        List<Disposable> disposables = new ArrayList<>();

        CountDownLatch subscribeCdl = new CountDownLatch(10);
        CountDownLatch dataCdl = new CountDownLatch(10);

        for (var i = 0; i < 10; i++) {
            Disposable disposable = client.get()
                    .uri("/test/sse")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                    })
                    .doOnSubscribe(s -> {
                        log.info("Subscribed: {}", s);
                        subscribeCdl.countDown();
                    })
                    .onErrorComplete()
                    .publishOn(Schedulers.boundedElastic())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(handler -> {
                        log.info("Data: {}", handler);
                        dataCdl.countDown();
                    });

            disposables.add(disposable);
        }

        try {
            var finished = subscribeCdl.await(30, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } catch (InterruptedException e) {
            fail(e);
        }

        try {
            var finished = dataCdl.await(30, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } catch (InterruptedException e) {
            fail(e);
        }

        applicationContext.stop();

        disposables.forEach(Disposable::dispose);
    }
}
