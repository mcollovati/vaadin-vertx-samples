package org.springframework.data.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface JpaRepository<E, ID> extends CrudRepository<E, ID> {

    Page<E> findAll(Pageable pageable);
}
