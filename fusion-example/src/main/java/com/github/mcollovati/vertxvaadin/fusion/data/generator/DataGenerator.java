package com.github.mcollovati.vertxvaadin.fusion.data.generator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import com.github.mcollovati.vertxvaadin.fusion.data.entity.Person;
import com.github.mcollovati.vertxvaadin.fusion.data.service.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataGenerator {

    public Consumer<Object[]> loadData(PersonRepository personRepository) {
        return args -> {
            Logger logger = LoggerFactory.getLogger(getClass());
            if (personRepository.count() != 0L) {
                logger.info("Using existing database");
                return;
            }
            logger.info("Generating demo data");

            logger.info("... generating 1000 Person entities...");
            List<Person> data = new ArrayList<>();
            try (Scanner sc = new Scanner(getClass().getResourceAsStream("/MOCK_DATA.csv")).useDelimiter("(,|\\n)")) {
                sc.nextLine();
                while (sc.hasNext()) {
                    Person p = new Person();
                    p.setId(sc.nextInt());
                    p.setFirstName(sc.next());
                    p.setLastName(sc.next());
                    p.setEmail(sc.next());
                    p.setPhone(sc.next());
                    p.setDateOfBirth(LocalDate.parse(sc.next(), DateTimeFormatter.ISO_DATE));
                    p.setOccupation(sc.next());
                    p.setImportant(sc.nextBoolean());
                    data.add(p);
                }
            }
            personRepository.saveAll(data);

            logger.info("Generated demo data");
        };
    }

}