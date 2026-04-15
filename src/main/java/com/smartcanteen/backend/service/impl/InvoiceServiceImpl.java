package com.smartcanteen.backend.service.impl;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.smartcanteen.backend.dto.response.InvoiceResponseDTO;
import com.smartcanteen.backend.dto.response.OrderItemDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.InvoiceService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public byte[] generateInvoice(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (currentUserEmail == null) {
            throw new RuntimeException("User not authenticated");
        }

        if (!isAdmin && !order.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Access denied");
        }

        if (order.getUser() == null) {
            throw new RuntimeException("Order user not found");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Smart Canteen Invoice").setBold().setFontSize(18));

            document.add(new Paragraph("Order ID: " + order.getId()));
            document.add(new Paragraph("Customer: " + order.getUser().getName()));
            document.add(new Paragraph("Email: " + order.getUser().getEmail()));

            for (OrderItem item : order.getOrderItems()) {
                document.add(new Paragraph(
                        item.getFoodItem().getName() +
                                " x" + item.getQuantity() +
                                " - ₹" + item.getFoodItem().getPrice()
                ));
            }

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice", e);
            throw new RuntimeException("Error generating invoice: " + e.getMessage(), e);
        }
    }

    public InvoiceResponseDTO getInvoiceData(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (currentUserEmail == null) {
            throw new RuntimeException("User not authenticated");
        }

        if (!isAdmin && !order.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Access denied");
        }

        List<OrderItemDTO> items = order.getOrderItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .name(item.getFoodItem().getName())
                        .quantity(item.getQuantity())
                        .price(item.getFoodItem().getPrice().doubleValue())
                        .build())
                .toList();

        return InvoiceResponseDTO.builder()
                .orderId(order.getId())
                .customerName(order.getUser().getName())
                .email(order.getUser().getEmail())
                .status(order.getStatus().name())
                .date(order.getCreatedAt().toString())
                .items(items)
                .totalAmount(order.getTotalAmount().doubleValue())
                .pickupCode(order.getPickupCode())
                .build();
    }
}