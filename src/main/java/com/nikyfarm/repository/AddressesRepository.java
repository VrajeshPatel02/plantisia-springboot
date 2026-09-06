package com.nikyfarm.repository;

import com.nikyfarm.entity.Addresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressesRepository extends JpaRepository<Addresses, Long> {


    List<Addresses> findByUserId(Long userId);

    Optional<Addresses> findByUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserId(Long userId);
}