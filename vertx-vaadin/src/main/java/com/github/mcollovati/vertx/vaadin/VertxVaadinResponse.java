package com.github.mcollovati.vertx.vaadin;


import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marco on 16/07/16.
 */
public class VertxVaadinResponse implements VaadinResponse {

    private final HttpServerResponse response;
    private final VertxVaadinService service;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    private Buffer outBuffer = Buffer.buffer();
    private boolean useOOS = false;
    private boolean useWriter = false;

    public VertxVaadinResponse(VertxVaadinService service, HttpServerResponse response) {
        this.response = response;
        this.service = service;
    }

    @Override
    public void setStatus(int statusCode) {
        response.setStatusCode(statusCode);
    }

    @Override
    public void setContentType(String contentType) {
        response.putHeader("Content-Type", contentType);
    }

    @Override
    public void setHeader(String name, String value) {
        response.putHeader(name, value);
    }

    @Override
    public void setDateHeader(String name, long timestamp) {
        response.putHeader(name, dateTimeFormatter.format(OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (useWriter) {
            throw new IllegalStateException("Using Writer");
        }
        useOOS = true;
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                outBuffer.appendByte((byte)b);
            }

            @Override
            public void close() throws IOException {
                System.out.println("OOS END RESPONSE");
                response.end(outBuffer);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (useOOS) {
            throw new IllegalStateException("Using OOS");
        }
        useWriter = true;
        return new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                outBuffer.appendString(Stream.of(cbuf).skip(off).limit(len)
                    .map(c -> c.toString())
                    .collect(Collectors.joining()));
            }

            @Override
            public void flush() throws IOException {
                System.out.println("WRITER FLUSH RESPONSE");
                response.write(outBuffer);
                outBuffer = Buffer.buffer();
            }

            @Override
            public void close() throws IOException {
                System.out.println("WRITER END RESPONSE");
                response.end(outBuffer);
            }
        });
    }

    @Override
    public void setCacheTime(long milliseconds) {
        // TODO
    }

    @Override
    public void sendError(int errorCode, String message) throws IOException {
        response.setStatusCode(errorCode).end(message);
    }

    @Override
    public VaadinService getService() {
        return service;
    }

    @Override
    public void addCookie(javax.servlet.http.Cookie cookie) {
        // TODO
    }

    @Override
    public void setContentLength(int len) {
        response.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(len));
    }

    void end() {
        System.out.println("================================= CHUNKED "+ response.isChunked());
        if (!response.ended() && !response.isChunked()) {
            response.end(outBuffer);
        }
    }
}
