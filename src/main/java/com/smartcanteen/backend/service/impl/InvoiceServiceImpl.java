package com.smartcanteen.backend.service.impl;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
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
import java.nio.file.AccessDeniedException;
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

            //  IMPORTANT: MONOSPACE FONT
            PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
            document.setFont(font);

            // ===== HEADER =====
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("                 SMART CANTEEN"));
            document.add(new Paragraph("                    INVOICE"));
            document.add(new Paragraph("--------------------------------------------------\n"));

            // ===== ORDER INFO =====
            document.add(new Paragraph(String.format("Order ID:        #%d", order.getId())));
            document.add(new Paragraph(String.format("Date:            %s", order.getCreatedAt())));
            document.add(new Paragraph("\n"));

            // ===== CUSTOMER =====
            document.add(new Paragraph(String.format("Customer Name:   %s", order.getUser().getName())));
            document.add(new Paragraph(String.format("Email:           %s", order.getUser().getEmail())));
            document.add(new Paragraph("\n"));

            // ===== ITEMS =====
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("ITEMS"));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph(String.format("%-20s %-10s %-10s", "Item Name", "Qty", "Price")));
            document.add(new Paragraph("--------------------------------------------------"));

            for (OrderItem item : order.getOrderItems()) {
                document.add(new Paragraph(
                        String.format("%-20s %-10d ₹%.2f",
                                item.getFoodItem().getName(),
                                item.getQuantity(),
                                item.getFoodItem().getPrice()
                        )
                ));
            }

            document.add(new Paragraph("--------------------------------------------------\n"));

            // ===== TOTAL =====
            document.add(new Paragraph(
                    String.format("TOTAL (Incl. Taxes):          ₹%.2f", order.getTotalAmount())
            ));

            document.add(new Paragraph("\n--------------------------------------------------"));

            // ===== PICKUP CODE =====
            document.add(new Paragraph(
                    String.format(" Pickup Code: %s", order.getPickupCode())
            ));

            document.add(new Paragraph("--------------------------------------------------\n"));

            // ===== QR CODE =====
            String baseUrl = "https://smart-canteen-backend-k235.onrender.com";
            String qrData = baseUrl + "/orders/verify?code=" + order.getPickupCode();

            BarcodeQRCode qrCode = new BarcodeQRCode(qrData);
            Image qrImage = new Image(qrCode.createFormXObject(pdf));

            qrImage.setWidth(120);
            qrImage.setHeight(120);
            qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER);

            document.add(new Paragraph("🔳 Scan for Pickup")
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(qrImage);

            document.add(new Paragraph("\n--------------------------------------------------"));

            // ===== FOOTER =====
            document.add(new Paragraph("Thank you for ordering with Smart Canteen!"));
            document.add(new Paragraph("Please show this QR or pickup code at the counter."));
            document.add(new Paragraph("--------------------------------------------------"));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice", e);
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
            try {
                throw new AccessDeniedException("Access denied");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
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