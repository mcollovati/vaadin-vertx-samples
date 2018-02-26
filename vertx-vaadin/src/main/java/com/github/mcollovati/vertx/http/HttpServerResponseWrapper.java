package com.github.mcollovati.vertx.http;

import io.vertx.core.http.HttpServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class HttpServerResponseWrapper implements HttpServerResponse {

    @Delegate
    private final HttpServerResponse delegate;
}
