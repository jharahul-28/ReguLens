package com.rj.ReguLens.advice;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Exclude Swagger, raw strings/resources, and existing ApiResponse instances
        String returnTypeName = returnType.getParameterType().getName();
        return !returnTypeName.startsWith("org.springdoc")
                && !returnTypeName.startsWith("org.springframework.boot.actuate");
    }

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?>) {
            return body;
        }
        if (body instanceof byte[] || body instanceof String) {
            return body;
        }
        return new ApiResponse<>(body);
    }
}
