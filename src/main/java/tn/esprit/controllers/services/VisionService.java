package tn.esprit.controllers.services;

import tn.esprit.util.MyConnection;
import tn.esprit.Models.User;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisionService {
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{2}[/-]\\d{2}[/-]\\d{4}|\\d{4}[/-]\\d{2}[/-]\\d{2})\\b");
    private static final Pattern ARABIC_DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s+([\\p{IsArabic}]+)\\s+(\\d{4})");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]{1,3}\\d{5,9}\\b");
    private static final Pattern CIN_PATTERN = Pattern.compile("\\b\\d{6,12}\\b");
    private static final Pattern NAME_CLEANUP_PATTERN = Pattern.compile("[^A-ZÀ-ÿ ]");
    private static final Pattern ARABIC_TEXT_PATTERN = Pattern.compile(".*[\\p{IsArabic}].*");
    private static final List<String> NAME_STOP_WORDS = List.of(
            "REPUBLIQUE", "REPUBLIC", "PASSPORT", "CARTE", "IDENTITE", "NATIONALE",
            "NATIONAL", "TUNISIENNE", "TUNISIAN", "DATE", "NAISSANCE", "BIRTH", "SEX", "SEXE"
    );
    private static final List<String> ARABIC_NAME_LABELS = List.of("الاسم", "الإسم", "اللقب");
    private static final List<String> ARABIC_DATE_LABELS = List.of("تاريخ الولادة", "الولادة");
    private static final List<String> ARABIC_STOP_WORDS = List.of(
            "الجمهورية", "التونسية", "بطاقة", "التعريف", "الوطنية", "اللقب", "الاسم", "الإسم", "تاريخ", "الولادة"
    );
    private static final Map<String, Month> ARABIC_MONTHS = createArabicMonths();

    // DB accessed via MyConnection.getInstance().getConnection()

    public VerificationResult verifyIdentity(User user, String documentType, Path imagePath) {
        if (user == null) {
            throw new IllegalArgumentException("A connected user is required.");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("Document type is required.");
        }
        if (imagePath == null || Files.notExists(imagePath)) {
            throw new IllegalArgumentException("The selected image does not exist.");
        }

        GoogleCredentials credentials = loadCredentials();

        String extractedText = extractText(imagePath, credentials);
        String normalizedType = documentType.trim().toUpperCase(Locale.ROOT);
        String fullName = extractFullName(extractedText);
        String birthDate = extractBirthDate(extractedText);
        String documentNumber = extractDocumentNumber(extractedText, normalizedType);
        boolean readable = !extractedText.isBlank();
        boolean verified = readable
                && !fullName.isBlank()
                && !birthDate.isBlank()
                && !documentNumber.isBlank();
        String statusMessage = verified
                ? "Identity document verified successfully."
                : readable
                ? "Document was read but required identity fields were incomplete."
                : "Document is not readable.";

        VerificationResult result = new VerificationResult(
                verified,
                extractedText,
                fullName,
                birthDate,
                documentNumber,
                statusMessage
        );
        saveVerification(user, normalizedType, result);
        return result;
    }

    private GoogleCredentials loadCredentials() {
        Properties properties = new Properties();

        try (InputStream stream = resolveConfigStream()) {
            if (stream == null) {
                throw new IllegalStateException("config.properties was not found.");
            }
            properties.load(stream);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load config.properties.", ex);
        }

        String credentialsPath = Optional.ofNullable(properties.getProperty("google.credentials.path"))
                .orElse("")
                .trim();
        if (credentialsPath.isBlank()) {
            throw new IllegalStateException("google.credentials.path is missing in config.properties.");
        }

        Path path = Path.of(credentialsPath);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path).normalize();
        }
        if (Files.notExists(path)) {
            throw new IllegalStateException("Google credentials file was not found: " + path);
        }

        try (InputStream credentialStream = Files.newInputStream(path)) {
            return GoogleCredentials.fromStream(credentialStream)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Google credentials from: " + path, ex);
        }
    }

    private InputStream resolveConfigStream() throws IOException {
        InputStream classpathStream = VisionService.class.getClassLoader().getResourceAsStream("config.properties");
        if (classpathStream != null) {
            return classpathStream;
        }

        Path fileSystemPath = Path.of(System.getProperty("user.dir"), "config.properties");
        if (Files.exists(fileSystemPath)) {
            return Files.newInputStream(fileSystemPath);
        }
        return null;
    }

    private String extractText(Path imagePath, GoogleCredentials credentials) {
        byte[] fileBytes;

        try {
            fileBytes = Files.readAllBytes(imagePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read the selected file: " + imagePath, ex);
        }

        if (fileBytes.length == 0) {
            throw new IllegalStateException("The selected file is empty.");
        }

        try {
            ByteString imageBytes = ByteString.copyFrom(fileBytes);
            Image image = Image.newBuilder().setContent(imageBytes).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
                if (response.getResponsesCount() == 0) {
                    return "";
                }

                var annotateResponse = response.getResponses(0);
                if (annotateResponse.hasError()) {
                    throw new IllegalStateException("Vision API error: " + annotateResponse.getError().getMessage());
                }
                if (annotateResponse.hasFullTextAnnotation()) {
                    return Optional.ofNullable(annotateResponse.getFullTextAnnotation().getText()).orElse("").trim();
                }

                List<EntityAnnotation> annotations = annotateResponse.getTextAnnotationsList();
                if (!annotations.isEmpty()) {
                    return Optional.ofNullable(annotations.get(0).getDescription()).orElse("").trim();
                }
                return "";
            }
        } catch (ApiException ex) {
            String detail = ex.getStatusCode() == null ? ex.getMessage() : ex.getStatusCode().toString();
            throw new IllegalStateException("Google Vision API request failed: " + detail, ex);
        } catch (IOException ex) {
            String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            throw new IllegalStateException("Unable to initialize Google Vision: " + detail, ex);
        }
    }

    private String extractFullName(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return "";
        }

        String arabicName = extractArabicFullName(extractedText);
        if (!arabicName.isBlank()) {
            return arabicName;
        }

        for (String rawLine : extractedText.split("\\R")) {
            String line = NAME_CLEANUP_PATTERN.matcher(rawLine.toUpperCase(Locale.ROOT)).replaceAll(" ").trim();
            if (line.length() < 5) {
                continue;
            }
            if (NAME_STOP_WORDS.stream().anyMatch(line::contains)) {
                continue;
            }
            String[] words = line.split("\\s+");
            if (words.length >= 2 && words.length <= 5) {
                return toDisplayCase(line);
            }
        }
        return "";
    }

    private String extractBirthDate(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return "";
        }

        String arabicDate = extractArabicBirthDate(extractedText);
        if (!arabicDate.isBlank()) {
            return arabicDate;
        }

        Matcher matcher = DATE_PATTERN.matcher(extractedText);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            String normalized = normalizeDate(candidate);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String extractDocumentNumber(String extractedText, String documentType) {
        if (extractedText == null || extractedText.isBlank()) {
            return "";
        }

        Pattern preferredPattern = "PASSPORT".equalsIgnoreCase(documentType) ? PASSPORT_PATTERN : CIN_PATTERN;
        Matcher preferredMatcher = preferredPattern.matcher(extractedText.toUpperCase(Locale.ROOT));
        if (preferredMatcher.find()) {
            return preferredMatcher.group();
        }

        Pattern fallbackPattern = "PASSPORT".equalsIgnoreCase(documentType) ? CIN_PATTERN : PASSPORT_PATTERN;
        Matcher fallbackMatcher = fallbackPattern.matcher(extractedText.toUpperCase(Locale.ROOT));
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group();
        }
        return "";
    }

    private String normalizeDate(String rawDate) {
        String sanitized = rawDate.replace('/', '-');
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate parsedDate = LocalDate.parse(sanitized, formatter);
                return parsedDate.toString();
            } catch (DateTimeParseException ignored) {
                // try the next formatter
            }
        }
        return "";
    }

    private String toDisplayCase(String upperText) {
        StringBuilder builder = new StringBuilder();
        for (String word : upperText.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return builder.toString();
    }

    private String extractArabicFullName(String extractedText) {
        List<String> lines = extractedText.lines()
                .map(line -> Optional.ofNullable(line).orElse("").trim())
                .filter(line -> !line.isBlank())
                .toList();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!containsAny(line, ARABIC_NAME_LABELS)) {
                continue;
            }

            for (int j = i + 1; j < lines.size() && j <= i + 3; j++) {
                String candidate = normalizeArabicCandidate(lines.get(j));
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        for (String line : lines) {
            String candidate = normalizeArabicCandidate(line);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private String extractArabicBirthDate(String extractedText) {
        List<String> lines = extractedText.lines()
                .map(line -> Optional.ofNullable(line).orElse("").trim())
                .filter(line -> !line.isBlank())
                .toList();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (containsAny(line, ARABIC_DATE_LABELS)) {
                String normalized = normalizeArabicDate(line);
                if (!normalized.isBlank()) {
                    return normalized;
                }
                if (i + 1 < lines.size()) {
                    normalized = normalizeArabicDate(lines.get(i + 1));
                    if (!normalized.isBlank()) {
                        return normalized;
                    }
                }
            }
        }

        for (String line : lines) {
            String normalized = normalizeArabicDate(line);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalizeArabicCandidate(String line) {
        if (!ARABIC_TEXT_PATTERN.matcher(line).matches()) {
            return "";
        }
        if (containsAny(line, ARABIC_STOP_WORDS)) {
            return "";
        }
        if (line.chars().anyMatch(Character::isDigit)) {
            return "";
        }

        String candidate = line.replaceAll("\\s+", " ").trim();
        return candidate.length() >= 4 ? candidate : "";
    }

    private String normalizeArabicDate(String line) {
        Matcher matcher = ARABIC_DATE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return "";
        }

        int day = Integer.parseInt(matcher.group(1));
        String monthToken = matcher.group(2).trim();
        int year = Integer.parseInt(matcher.group(3));
        Month month = ARABIC_MONTHS.get(monthToken);
        if (month == null) {
            return "";
        }

        try {
            return LocalDate.of(year, month, day).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean containsAny(String value, List<String> tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Month> createArabicMonths() {
        Map<String, Month> months = new LinkedHashMap<>();
        months.put("جانفي", Month.JANUARY);
        months.put("فيفري", Month.FEBRUARY);
        months.put("مارس", Month.MARCH);
        months.put("افريل", Month.APRIL);
        months.put("أفريل", Month.APRIL);
        months.put("ماي", Month.MAY);
        months.put("جوان", Month.JUNE);
        months.put("يونيو", Month.JUNE);
        months.put("جويلية", Month.JULY);
        months.put("يوليو", Month.JULY);
        months.put("اوت", Month.AUGUST);
        months.put("أوت", Month.AUGUST);
        months.put("سبتمبر", Month.SEPTEMBER);
        months.put("اكتوبر", Month.OCTOBER);
        months.put("أكتوبر", Month.OCTOBER);
        months.put("نوفمبر", Month.NOVEMBER);
        months.put("ديسمبر", Month.DECEMBER);
        return months;
    }

    private void saveVerification(User user, String documentType, VerificationResult result) {
        if (!user.databaseBacked()) {
            throw new IllegalStateException("Identity verification can only be saved for MySQL users.");
        }

        String sql = """
                INSERT INTO identity_verifications (user_id, document_type, extracted_text, verification_status, created_at)
                VALUES (?, ?, ?, ?, NOW())
                """;
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS identity_verifications (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    document_type VARCHAR(50) NOT NULL,
                    extracted_text LONGTEXT NOT NULL,
                    verification_status VARCHAR(20) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_identity_verifications_user_id (user_id)
                )
                """;

        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ensureIdentityVerificationTable(connection, createTableSql);
            statement.setInt(1, Integer.parseInt(user.id()));
            statement.setString(2, documentType);
            statement.setString(3, result.extractedText());
            statement.setString(4, result.verified() ? "verified" : "rejected");
            statement.executeUpdate();
        } catch (Exception ex) {
            String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            throw new IllegalStateException("Unable to save identity verification: " + detail, ex);
        }
    }

    private void ensureIdentityVerificationTable(Connection connection, String createTableSql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(createTableSql)) {
            statement.execute();
        }
    }

    public record VerificationResult(
            boolean verified,
            String extractedText,
            String fullName,
            String birthDate,
            String documentNumber,
            String statusMessage
    ) {
    }
}
