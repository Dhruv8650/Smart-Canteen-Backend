package com.smartcanteen.backend.service.impl;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;

@Slf4j
@AllArgsConstructor
@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    @Override
    public byte[] generateInvoice(Long orderId) {

        log.info("Generating invoice for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isAdmin();

        // AUTH CHECK
        if (currentUserEmail == null && !isAdmin) {
            throw new RuntimeException("User not authenticated");
        }

        // ROLE CHECK
        if (!isAdmin && !order.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Access denied");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            //  TITLE
            document.add(new Paragraph("Smart Canteen Invoice")
                    .setBold()
                    .setFontSize(18));

            //  BASIC DETAILS
            document.add(new Paragraph("Order ID: " + order.getId()));
            document.add(new Paragraph("Customer: " + order.getUser().getName()));
            document.add(new Paragraph("Email: " + order.getUser().getEmail()));
            document.add(new Paragraph("Date: " + order.getCreatedAt()));
            document.add(new Paragraph("Status: " + order.getStatus()));

            document.add(new Paragraph("\nItems:").setBold());

            // 🔹 ITEMS
            for (OrderItem item : order.getOrderItems()) {
                document.add(new Paragraph(
                        item.getFoodItem().getName() +
                                " x" + item.getQuantity() +
                                " - ₹" + item.getFoodItem().getPrice()
                ));
            }

            // 🔹 TOTAL
            document.add(new Paragraph("\nTotal Amount: ₹" + order.getTotalAmount())
                    .setBold());

            //  OPTIONAL: ADD PICKUP CODE
            if (order.getPickupCode() != null) {
                document.add(new Paragraph("Pickup Code: " + order.getPickupCode()));
            }

            document.close();

            log.info("PDF invoice generated for order ID: {}", orderId);

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice", e);
            throw new RuntimeException("Error generating invoice");
        }
    }
}