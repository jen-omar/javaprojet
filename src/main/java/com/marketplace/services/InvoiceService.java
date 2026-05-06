package com.marketplace.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

public class InvoiceService {

    public void generateInvoice(Order order, Product product) {
        try {
            // 1. Create directory if not exists
            File dir = new File("invoices");
            if (!dir.exists()) dir.mkdir();

            String fileName = "invoices/facture_" + order.getId() + ".pdf";
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(fileName));

            document.open();

            // 2. Styling
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font goldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(201, 168, 76));

            // 3. Header
            Paragraph brand = new Paragraph("MYTHORIA MARKETPLACE", titleFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);
            
            Paragraph sub = new Paragraph("Facture Officielle & Bon de Retrait", headerFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);
            
            document.add(new Paragraph("\n"));
            document.add(new LineSeparator());
            document.add(new Paragraph("\n"));

            // 4. Content
            document.add(new Paragraph("Informations de Commande :", headerFont));
            document.add(new Paragraph("N° de Commande : #" + order.getId(), normalFont));
            document.add(new Paragraph("Date : " + (order.getCreatedAt() != null ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Maintenant"), normalFont));
            document.add(new Paragraph("Client : " + order.getBuyerName(), normalFont));
            
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Détails du Produit :", headerFont));
            document.add(new Paragraph("Nom : " + product.getName(), normalFont));
            document.add(new Paragraph("Artiste : " + (product.getArtistName() != null ? product.getArtistName() : "Anonyme"), normalFont));
            document.add(new Paragraph("Type : " + product.getType(), normalFont));
            
            document.add(new Paragraph("\n"));
            Paragraph total = new Paragraph("TOTAL PAYÉ : " + order.getPrice() + " €", goldFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph("\n\n"));
            document.add(new LineSeparator());
            document.add(new Paragraph("\n"));

            // 5. QR Code for Pickup (Enriched with more details)
            Paragraph pickupTitle = new Paragraph("PRÉSENTEZ CE CODE QR POUR LE RETRAIT", headerFont);
            pickupTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pickupTitle);
            
            String dateStr = (order.getCreatedAt() != null) 
                             ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) 
                             : "Date non spécifiée";

            String qrData = "=== Informations de Commande ===\n" +
                            "Commande N° : #" + order.getId() + "\n" +
                            "Client : " + order.getBuyerName() + "\n" +
                            "Produit : " + product.getName() + "\n" +
                            "PRIX TOTAL : " + order.getPrice() + " EUR\n" +
                            "Statut : Payé & Vérifié";
            
            byte[] qrImageBytes = generateQRCodeImage(qrData);
            
            Image qrImage = Image.getInstance(qrImageBytes);
            qrImage.scaleAbsolute(150, 150);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            Paragraph footer = new Paragraph("\n\nMerci de votre confiance. Mythoria - L'art entre vos mains.", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // 6. Open the PDF
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(fileName));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] generateQRCodeImage(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 350, 350);
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}
