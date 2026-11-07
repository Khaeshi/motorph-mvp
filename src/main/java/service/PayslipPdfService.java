package com.motorph.service;

import com.motorph.model.Payslip;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Control 5: the PDF is built and returned as a byte array. Nothing is written
 * to a file. (PDFBox 3 keeps document streams in memory by default.)
 */
@Service
public class PayslipPdfService {

    public byte[] generate(Payslip slip) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.setLeading(18f);
                cs.newLineAtOffset(72, 720);
                for (String line : lines(slip)) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    private static List<String> lines(Payslip s) {
        return List.of(
                "MotorPH Payslip",
                "Date: " + s.generatedOn(),
                "Employee: " + s.employee().id() + " - " + ascii(s.employee().fullName()),
                "Position: " + ascii(s.employee().jobTitle()),
                " ",
                "Basic salary: " + money(s.employee().basicSalary()),
                "Allowance: " + money(s.employee().allowance()),
                "Gross pay: " + money(s.gross()),
                "SSS: " + money(s.sss()),
                "PhilHealth: " + money(s.philhealth()),
                "Pag-IBIG: " + money(s.pagibig()),
                "Withholding tax: " + money(s.tax()),
                "Total deductions: " + money(s.totalDeductions()),
                "Net pay: " + money(s.net()),
                " ",
                "Simplified demo computation.");
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.US, "PHP %,.2f", v);
    }

    /** The standard PDF font only accepts basic characters; anything else becomes '?'. */
    private static String ascii(String s) {
        return s.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
