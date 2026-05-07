package tn.esprit.controllers;

import tn.esprit.Models.Wallet;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WalletExportService {
    public void exportExcel(List<Wallet> wallets, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Wallets");
            String[] columns = {"ID", "Updated At", "User ID", "Solde", "Statut", "Devise", "Plafond"};

            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            for (int i = 0; i < wallets.size(); i++) {
                Wallet wallet = wallets.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(wallet.id());
                row.createCell(1).setCellValue(wallet.updatedAt());
                row.createCell(2).setCellValue(wallet.userId());
                row.createCell(3).setCellValue(wallet.balance());
                row.createCell(4).setCellValue(wallet.status());
                row.createCell(5).setCellValue(wallet.currency());
                row.createCell(6).setCellValue(wallet.ceiling());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(target)) {
                workbook.write(outputStream);
            }
        }
    }

    public void exportPdf(List<Wallet> wallets, Path target) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float startX = 40;
            float y = 800;
            float rowHeight = 18;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(startX, y);
                content.showText("Wallet Export");
                content.endText();
                y -= 28;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(startX, y);
                content.showText("ID | Updated At | User ID | Solde | Statut | Devise | Plafond");
                content.endText();
                y -= rowHeight;

                for (Wallet wallet : wallets) {
                    if (y < 60) {
                        break;
                    }

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(startX, y);
                    content.showText(formatWalletRow(wallet));
                    content.endText();
                    y -= rowHeight;
                }
            }

            document.save(target.toFile());
        }
    }

    private String formatWalletRow(Wallet wallet) {
        return sanitize(wallet.id() + " | "
                + wallet.updatedAt() + " | "
                + wallet.userId() + " | "
                + wallet.balance() + " | "
                + wallet.status() + " | "
                + wallet.currency() + " | "
                + wallet.ceiling());
    }

    private String sanitize(String value) {
        return value.replace("\n", " ").replace("\r", " ");
    }
}
