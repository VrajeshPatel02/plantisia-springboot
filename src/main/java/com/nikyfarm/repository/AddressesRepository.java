package com.nikyfarm.repository;

import com.nikyfarm.entity.Addresses;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressesRepository extends JpaRepository<Addresses, Long> {
}
