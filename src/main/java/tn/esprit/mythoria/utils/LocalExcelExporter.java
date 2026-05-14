package tn.esprit.mythoria.utils;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.esprit.mythoria.entity.Local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public final class LocalExcelExporter {

    private LocalExcelExporter() {
    }

    public static void exportToExcel(List<Local> locals, File outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Locaux");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Description");
            header.createCell(3).setCellValue("Price");
            header.createCell(4).setCellValue("Address");
            header.createCell(5).setCellValue("Capacity");
            header.createCell(6).setCellValue("Image");
            header.createCell(7).setCellValue("Status");

            int rowIndex = 1;
            for (Local local : locals) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(local.getId());
                row.createCell(1).setCellValue(local.getName() != null ? local.getName() : "");
                row.createCell(2).setCellValue(local.getDescription() != null ? local.getDescription() : "");
                row.createCell(3).setCellValue(local.getPrice());
                row.createCell(4).setCellValue(local.getAddress() != null ? local.getAddress() : "");
                row.createCell(5).setCellValue(local.getCapacity());
                row.createCell(6).setCellValue(local.getImage() != null ? local.getImage() : "");
                row.createCell(7).setCellValue(local.getStatus() != null ? local.getStatus() : "");
            }

            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
}

