package com.github.mcollovati.vertxvaadin.fusion.data.service;

import com.github.mcollovati.vertxvaadin.fusion.data.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Integer> {

}