package com.github.mcollovati.vertxvaadin.fusion.data.service;

import com.github.mcollovati.vertxvaadin.fusion.data.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {

}