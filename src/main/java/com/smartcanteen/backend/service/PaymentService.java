package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.request.PaymentVerifyRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> createPaymentOrder(OrderRequestDTO request, String userEmail);

    OrderResponseDTO verifyPayment(PaymentVerifyRequestDTO request, String userEmail);

    void handleWebhook(String payload, String signature);
}
