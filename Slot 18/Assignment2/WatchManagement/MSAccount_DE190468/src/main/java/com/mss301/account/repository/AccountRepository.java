package com.mss301.account.repository;

import com.mss301.account.entity.StoreAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// [TODO-3.1] The identity provider reads only DBAccount.
public interface AccountRepository extends JpaRepository<StoreAccount, Integer> {
  Optional<StoreAccount> findByUsername(String username);
}
