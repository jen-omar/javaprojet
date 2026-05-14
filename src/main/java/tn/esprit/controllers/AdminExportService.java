package tn.esprit.controllers;

import tn.esprit.Models.User;
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

public final class AdminExportService {
    public void exportUsersExcel(List<User> users, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Users");
            String[] columns = {"ID", "Username", "Email", "Prenom", "Nom", "Telephone", "Role", "Score", "Rank"};

            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(user.id());
                row.createCell(1).setCellValue(user.username());
                row.createCell(2).setCellValue(user.email());
                row.createCell(3).setCellValue(user.firstName());
                row.createCell(4).setCellValue(user.lastName());
                row.createCell(5).setCellValue(user.phoneNumber());
                row.createCell(6).setCellValue(user.role());
                row.createCell(7).setCellValue(user.points());
                row.createCell(8).setCellValue(user.rank());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(target)) {
                workbook.write(outputStream);
            }
        }
    }

    public void exportWalletsExcel(List<Wallet> wallets, Path target) throws IOException {
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

    public void exportUsersPdf(List<User> users, Path target) throws IOException {
        exportPdfLines(users.stream()
                .map(user -> sanitize(user.id() + " | " + user.username() + " | " + user.email() + " | "
                        + user.firstName() + " | " + user.lastName() + " | " + user.phoneNumber() + " | "
                        + user.role() + " | " + user.points() + " | " + user.rank()))
                .toList(), "Users Export", target);
    }

    public void exportWalletsPdf(List<Wallet> wallets, Path target) throws IOException {
        exportPdfLines(wallets.stream()
                .map(wallet -> sanitize(wallet.id() + " | " + wallet.updatedAt() + " | " + wallet.userId() + " | "
                        + wallet.balance() + " | " + wallet.status() + " | " + wallet.currency() + " | " + wallet.ceiling()))
                .toList(), "Wallets Export", target);
    }

    private void exportPdfLines(List<String> lines, String title, Path target) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float x = 40;
                float y = 800;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(x, y);
                content.showText(title);
                content.endText();
                y -= 28;

                for (String line : lines) {
                    if (y < 60) {
                        break;
                    }
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 8);
                    content.newLineAtOffset(x, y);
                    content.showText(line);
                    content.endText();
                    y -= 16;
                }
            }

            document.save(target.toFile());
        }
    }

    private String sanitize(String value) {
        return value.replace("\n", " ").replace("\r", " ");
    }
}
