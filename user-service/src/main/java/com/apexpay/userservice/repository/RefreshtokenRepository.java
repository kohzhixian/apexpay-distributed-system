package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshtokenRepository extends JpaRepository<RefreshToken, String> {
}
