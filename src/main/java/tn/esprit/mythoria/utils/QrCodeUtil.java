package tn.esprit.mythoria.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

public final class QrCodeUtil {
    private static final int DEFAULT_SIZE = 300;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Paths.get("qrcodes");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private QrCodeUtil() {
    }

    public static String generateQrCode(String content) throws IOException, WriterException {
        String fileName = "qr-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".png";
        return generateQrCode(content, DEFAULT_OUTPUT_DIRECTORY.resolve(fileName));
    }

    public static String generateQrCode(String content, String fileNameOrPath) throws IOException, WriterException {
        return generateQrCode(content, resolveOutputFile(fileNameOrPath));
    }

    public static String generateQrCode(String content, Path outputFile) throws IOException, WriterException {
        return generateQrCode(content, outputFile, DEFAULT_SIZE);
    }

    public static String generateQrCode(String content, Path outputFile, int size) throws IOException, WriterException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("QR code content must not be empty.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("QR code output file must not be null.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("QR code size must be positive.");
        }

        Path pngOutputFile = ensurePngExtension(outputFile).toAbsolutePath().normalize();
        Path parent = pngOutputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        ImageIO.write(toImage(bitMatrix), "PNG", pngOutputFile.toFile());

        return pngOutputFile.toString();
    }

    private static BufferedImage toImage(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);
            }
        }

        return image;
    }

    private static Path resolveOutputFile(String fileNameOrPath) {
        if (fileNameOrPath == null || fileNameOrPath.isBlank()) {
            throw new IllegalArgumentException("QR code file name must not be empty.");
        }

        Path outputFile = Paths.get(fileNameOrPath.trim());
        if (outputFile.getParent() == null) {
            outputFile = DEFAULT_OUTPUT_DIRECTORY.resolve(outputFile);
        }
        return outputFile;
    }

    private static Path ensurePngExtension(Path outputFile) {
        String fileName = outputFile.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".png")) {
            return outputFile;
        }

        Path parent = outputFile.getParent();
        Path pngFileName = Paths.get(fileName + ".png");
        return parent == null ? pngFileName : parent.resolve(pngFileName);
    }
}
