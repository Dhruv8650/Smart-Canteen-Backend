package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface BlackListedTokenRepository extends JpaRepository<BlackListedToken,String> {

}
