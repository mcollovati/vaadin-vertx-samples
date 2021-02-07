package com.github.mcollovati.vertxvaadin.fusion.data.service;

import com.github.mcollovati.vertxvaadin.fusion.data.entity.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.artur.helpers.CrudService;

public class PersonService extends CrudService<Person, Integer> {

    private PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    protected PersonRepository getRepository() {
        return repository;
    }

}
