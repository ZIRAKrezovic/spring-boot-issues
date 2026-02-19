package com.example.demo;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableChannel;
import reactor.netty.http.server.HttpServerResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// @Component
public class NettyShutdownFilter implements WebFilter, SmartLifecycle {
    private final Sinks.Many<Boolean> shutdownSink = Sinks.many().replay().latestOrDefault(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        running.compareAndSet(false, true);
    }

    @Override
    public void stop() {
        shutdownSink.tryEmitNext(Boolean.TRUE);
        running.compareAndSet(true, false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return SmartLifecycle.DEFAULT_PHASE - 512;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpServerResponse nettyRes = unwrapNettyResponse(exchange);

        if (nettyRes == null) {
            return chain.filter(exchange);
        }

        List<MediaType> accept = exchange.getRequest().getHeaders().getAccept();

        if (accept.stream().anyMatch(MediaType.TEXT_EVENT_STREAM::includes)) {
            Disposable disposable =
                    shutdownSink
                            .asFlux()
                            .filter(Boolean::booleanValue)
                            .next()
                            .flatMap(i -> exchange.getResponse().setComplete())
                            .doOnSuccess(i -> nettyRes.withConnection(DisposableChannel::dispose))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();

            return chain.filter(exchange)
                    .doOnSuccess(i -> disposable.dispose())
                    .doOnError(i -> disposable.dispose());
        }

        return chain.filter(exchange);
    }

    private HttpServerResponse unwrapNettyResponse(ServerWebExchange exchange) {
        ServerHttpResponse res = exchange.getResponse();

        while (res instanceof ServerHttpResponseDecorator dec) {
            res = dec.getDelegate();
        }

        if (res instanceof AbstractServerHttpResponse ar
                && ar.getNativeResponse() instanceof HttpServerResponse nr) {
            return nr;
        }

        return null;
    }
}

