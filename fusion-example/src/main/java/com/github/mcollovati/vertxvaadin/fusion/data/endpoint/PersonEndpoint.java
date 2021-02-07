package com.github.mcollovati.vertxvaadin.fusion.data.endpoint;

import com.github.mcollovati.vertxvaadin.fusion.AppVerticle;
import com.github.mcollovati.vertxvaadin.fusion.data.CrudEndpoint;
import com.github.mcollovati.vertxvaadin.fusion.data.entity.Person;
import com.github.mcollovati.vertxvaadin.fusion.data.service.PersonService;
import com.vaadin.flow.server.connect.Endpoint;

@Endpoint
public class PersonEndpoint extends CrudEndpoint<Person, Integer> {

    private PersonService service;

    public PersonEndpoint(PersonService service) {
        this.service = service;
    }

    public PersonEndpoint() {
        this(new PersonService(AppVerticle.personRepo));
    }

    @Override
    protected PersonService getService() {
        return service;
    }

}
