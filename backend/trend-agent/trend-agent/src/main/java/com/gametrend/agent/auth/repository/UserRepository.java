package com.gametrend.agent.auth.repository;

import com.gametrend.agent.auth.entity.UserAccount;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
