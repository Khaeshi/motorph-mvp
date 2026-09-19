package com.motorph.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    @GetMapping("/")
    public String home() {
        return "MotorPH MVP is running. Go to /login to sign in.";
    }

    @GetMapping("/employee/dashboard")
    public String employeeDashboard(Authentication auth) {
        return "Employee dashboard — hello " + auth.getName()
                + ". Authorities: " + auth.getAuthorities();
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication auth) {
        return "Admin dashboard — hello " + auth.getName()
                + ". Authorities: " + auth.getAuthorities();
    }

    @GetMapping("/employee/payslip")
    public ResponseEntity<byte[]> payslip(Authentication auth) throws IOException {
        byte[] pdfBytes;

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText("MotorPH Payslip");
                contentStream.newLineAtOffset(0, -24);
                contentStream.showText("Username: " + auth.getName());
                contentStream.newLineAtOffset(0, -24);
                contentStream.showText("Gross Pay: PHP 25,000.00");
                contentStream.newLineAtOffset(0, -24);
                contentStream.showText("Net Pay: PHP 22,150.00");
                contentStream.newLineAtOffset(0, -24);
                contentStream.showText("Date: " + LocalDate.now());
                contentStream.endText();
            }

            document.save(baos);
            pdfBytes = baos.toByteArray();
        }

        audit.info("action=PAYSLIP_EXPORT actor={} timestamp={}", auth.getName(), Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("payslip.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
