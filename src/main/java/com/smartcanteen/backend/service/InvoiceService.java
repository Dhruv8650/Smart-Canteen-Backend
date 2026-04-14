package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Order;

public interface InvoiceService {
    byte[] generateInvoice(Long orderId);
}