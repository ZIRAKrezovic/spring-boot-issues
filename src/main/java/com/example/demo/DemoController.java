package com.example.demo;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("test")
public class DemoController {
    public static final Sinks.Many<String> sink = Sinks.many().replay().latestOrDefault("SSE Hello");

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "sse")
    Flux<ServerSentEvent<String>> sseHandler() {
        return sink.asFlux().map(s -> ServerSentEvent.<String>builder().event("message").data(s).build());
    }
}
