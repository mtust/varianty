package com.tustanovskyy.varianty.repository;

import com.tustanovskyy.varianty.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
