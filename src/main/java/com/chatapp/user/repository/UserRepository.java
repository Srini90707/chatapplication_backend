package com.chatapp.user.repository;

import com.chatapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByNumber(String number);

    boolean existsByNumber(String number);

    List<User> findByNumberNot(String number);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.number = :number OR (LENGTH(u.number) >= 10 AND LENGTH(:number) >= 10 AND RIGHT(u.number, 10) = RIGHT(:number, 10))")
    Optional<User> findByPhoneNormalized(@org.springframework.data.repository.query.Param("number") String number);
}
