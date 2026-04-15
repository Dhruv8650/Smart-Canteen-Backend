package com.smartcanteen.backend.service.impl;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import com.smartcanteen.backend.dto.response.InvoiceResponseDTO;
import com.smartcanteen.backend.dto.response.OrderItemDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.InvoiceService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    @Transactional
    @Override
    public byte[] generateInvoice(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // ===== HEADER =====
            document.add(new Paragraph("Smart Canteen")
                    .setBold()
                    .setFontSize(20));

            document.add(new Paragraph("Invoice")
                    .setFontSize(14));

            document.add(new Paragraph("\n"));

            // ===== ORDER INFO =====
            document.add(new Paragraph("Order ID: " + order.getId()));
            document.add(new Paragraph("Date: " + order.getCreatedAt()));
            document.add(new Paragraph("\n"));

            // ===== CUSTOMER =====
            document.add(new Paragraph("Customer: " + order.getUser().getName()));
            document.add(new Paragraph("Email: " + order.getUser().getEmail()));
            document.add(new Paragraph("\n"));

            // ===== TABLE =====
            float[] columnWidths = {200F, 100F, 100F};
            Table table = new Table(columnWidths);

            table.addCell("Item");
            table.addCell("Qty");
            table.addCell("Price");

            for (OrderItem item : order.getOrderItems()) {
                table.addCell(item.getFoodItem().getName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell("₹" + item.getFoodItem().getPrice());
            }

            document.add(table);

            document.add(new Paragraph("\n"));

            // ===== TOTAL =====
            document.add(new Paragraph("Total (Incl. Taxes): ₹" + order.getTotalAmount())
                    .setBold());

            document.add(new Paragraph("\n"));

            // ===== PICKUP CODE =====
            document.add(new Paragraph("Pickup Code: " + order.getPickupCode()));

            document.add(new Paragraph("\n"));

            // ===== QR CODE =====
            String qrUrl = "https://smart-canteen-backend-k235.onrender.com/orders/verify?code=" + order.getPickupCode();

            BarcodeQRCode qrCode = new BarcodeQRCode(qrUrl);
            Image qrImage = new Image(qrCode.createFormXObject(pdf));

            qrImage.setWidth(120);
            qrImage.setHeight(120);

            document.add(new Paragraph("Scan for Pickup"));
            document.add(qrImage);

            document.add(new Paragraph("\n"));

            // ===== FOOTER =====
            document.add(new Paragraph("Thank you for ordering with Smart Canteen!")
                    .setFontSize(10));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice");
        }
    }

    // OPTIONAL: API data for frontend
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