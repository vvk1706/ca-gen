package com.dlis.core.dao;

import com.dlis.core.domain.AuthorityUser;
import java.util.Optional;

/**
 * DAO interface for DLIS.AUTHORITY_USER table.
 */
public interface AuthorityUserDao {

    Optional<AuthorityUser> findByUserCodeAndLevel(String userCode, String authorityLevel);
}
