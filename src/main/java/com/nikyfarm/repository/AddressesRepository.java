package com.nikyfarm.repository;

import com.nikyfarm.entity.Addresses;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressesRepository extends JpaRepository<Addresses, Long> {

    List<Addresses> findByUserId(Long userId);

    Optional<Addresses> findByUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserId(Long userId);

    Optional<Addresses> findByIdAndUserId(Long id, Long userId);
}
