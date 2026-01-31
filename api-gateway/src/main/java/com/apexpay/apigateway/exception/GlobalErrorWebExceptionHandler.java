package com.apexpay.apigateway.exception;

import com.apexpay.apigateway.config.GlobalErrorAttributes;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Global error handler for WebFlux reactive error processing.
 * Catches all unhandled exceptions and renders them as JSON responses
 * using {@link GlobalErrorAttributes} for error mapping.
 * Order(-2) ensures this handler runs before Spring's default error handler.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    /**
     * Constructs the global error handler with required dependencies.
     *
     * @param globalErrorAttributes custom error attributes mapper
     * @param applicationContext    Spring application context
     * @param serverCodecConfigurer codec configurer for message readers/writers
     */
    public GlobalErrorWebExceptionHandler(GlobalErrorAttributes globalErrorAttributes,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(globalErrorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    /**
     * Creates the routing function that handles all error requests.
     *
     * @param errorAttributes the error attributes to use for building responses
     * @return router function that routes all requests to the error renderer
     */
    @Override
    @NullMarked
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * Renders the error response as JSON with appropriate HTTP status.
     *
     * @param request the server request that caused the error
     * @return Mono containing the JSON error response
     */
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        int status = (int) errorPropertiesMap.getOrDefault("status", 500);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }
}
