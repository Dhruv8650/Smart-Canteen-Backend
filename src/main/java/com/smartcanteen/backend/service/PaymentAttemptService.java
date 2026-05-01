package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.PaymentAttempt;
import com.smartcanteen.backend.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAttemptService {

    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt saveAttempt(PaymentAttempt attempt) {
        return paymentAttemptRepository.save(attempt);
    }
}
