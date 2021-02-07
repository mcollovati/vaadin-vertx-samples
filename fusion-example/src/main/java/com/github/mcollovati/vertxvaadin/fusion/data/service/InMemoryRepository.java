package com.github.mcollovati.vertxvaadin.fusion.data.service;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.mcollovati.vertxvaadin.fusion.data.AbstractEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public class InMemoryRepository<E extends AbstractEntity> implements JpaRepository<E, Integer> {

    private final AtomicInteger idGenerator = new AtomicInteger();
    private final Map<Integer, E> data = new LinkedHashMap<>();
    private final Map<String, PropertyDescriptor> propertyDescriptors;

    @SuppressWarnings("rawtypes")
    protected InMemoryRepository() {
        Class<?> type = (Class) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        propertyDescriptors = inspect(type);
    }

    private static Map<String, PropertyDescriptor> inspect(Class<?> entityType) {
        try {
            return Stream.of(Introspector.getBeanInfo(entityType).getPropertyDescriptors())
                .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<E> findAll(Pageable pageable) {
        return new PageImpl<>(
            data.values().stream()
                .sorted(orderBy(pageable.getSort()))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList()), pageable, data.size());

    }

    @SuppressWarnings("unchecked")
    private Comparator<E> orderBy(Sort sort) {
        return sort.stream()
            .filter(order -> propertyDescriptors.containsKey(order.getProperty()))
            .filter(order -> Comparable.class.isAssignableFrom(propertyDescriptors.get(order.getProperty()).getPropertyType()))
            .map(order -> {
                PropertyDescriptor descriptor = propertyDescriptors.get(order.getProperty());
                Comparator<E> comparator = Comparator.comparing(
                    o -> {
                        try {
                            return (Comparable<Object>) descriptor.getReadMethod().invoke(o);
                        } catch (Exception e) {
                            return null;
                        }
                    },
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                if (order.isDescending()) {
                    comparator = comparator.reversed();
                }
                return comparator;
            }).reduce(Comparator::thenComparing)
            .orElse((o1, o2) -> 0);
    }


    @Override
    public <S extends E> S save(S entity) {
        if (entity.isNew()) {
            entity.setId(idGenerator.incrementAndGet());
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends E> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<E> findById(Integer id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public boolean existsById(Integer id) {
        return data.containsKey(id);
    }

    @Override
    public Iterable<E> findAll() {
        return data.values();
    }

    @Override
    public Iterable<E> findAllById(Iterable<Integer> ids) {
        return StreamSupport.stream(ids.spliterator(), false)
            .map(data::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return data.size();
    }

    @Override
    public void deleteById(Integer id) {
        data.remove(id);
    }

    @Override
    public void delete(E entity) {
        if (!entity.isNew()) {
            data.remove(entity.getId());
        }
    }

    @Override
    public void deleteAll(Iterable<? extends E> entities) {
        StreamSupport.stream(entities.spliterator(), false)
            .filter(e -> e != null && !e.isNew())
            .map(AbstractEntity::getId)
            .forEach(data::remove);
    }

    @Override
    public void deleteAll() {
        data.clear();
    }
}
