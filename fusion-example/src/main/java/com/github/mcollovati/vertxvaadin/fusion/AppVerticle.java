package com.github.mcollovati.vertxvaadin.fusion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertxvaadin.fusion.data.generator.DataGenerator;
import com.github.mcollovati.vertxvaadin.fusion.data.service.InMemoryPersonRepository;
import com.github.mcollovati.vertxvaadin.fusion.data.service.PersonRepository;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class AppVerticle extends VaadinVerticle {

    public static final PersonRepository personRepo = new InMemoryPersonRepository();
    @Override
    public void start() {
        DatabindCodec.mapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        ;
        new DataGenerator().loadData(personRepo).accept(new Object[0]);
    }

    @Override
    protected void serviceInitialized(VertxVaadinService service, Router router) {
        super.serviceInitialized(service, router);
    }
}
