package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.PaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttempt> findByGatewayOrderId(String gatewayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttempt> findByGatewayOrderIdAndUserEmail(String gatewayOrderId, String email);


    boolean existsByGatewayPaymentId(String gatewayPaymentId);
}
