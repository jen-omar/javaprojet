package tn.esprit.controllers;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\u00C0-\\u00FF][A-Za-z\\u00C0-\\u00FF' -]{1,49}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+216\\s[0-9]{8}$");
    private static final Pattern PHONE_INPUT_PATTERN = Pattern.compile("^$|^\\+$|^\\+2$|^\\+21$|^\\+216$|^\\+216\\s$|^\\+216\\s?[0-9]{0,8}$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern STATUS_PATTERN = Pattern.compile("^[A-Za-z\\u00C0-\\u00FF][A-Za-z\\u00C0-\\u00FF _-]{1,29}$");
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "admin", "author", "client");

    private ValidationUtils() {
    }

    public static String requireUsername(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim().toLowerCase();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Username invalide. Utilise 3 a 30 caracteres: lettres, chiffres, point, tiret ou underscore.");
        }
        return normalized;
    }

    public static String requireEmail(String value) {
        String normalized = normalizeEmail(value);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email invalide.");
        }
        return normalized;
    }

    public static String optionalName(String value, String fieldLabel) {
        String normalized = Optional.ofNullable(value).orElse("").trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldLabel + " invalide.");
        }
        return normalized;
    }

    public static String optionalPhone(String value) {
        String normalized = normalizePhone(value);
        if (normalized.isBlank()) {
            return "";
        }
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Numero de telephone invalide. Utilise le format +216 94946268.");
        }
        return normalized;
    }

    public static String requireStrongPassword(String value) {
        String password = Optional.ofNullable(value).orElse("");
        if (password.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caracteres.");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une majuscule.");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une minuscule.");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un chiffre.");
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un caractere special.");
        }
        return password;
    }

    public static String requireCurrency(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim().toUpperCase();
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Devise invalide. Utilise un code sur 3 lettres comme TND ou EUR.");
        }
        return normalized;
    }

    public static String requireStatus(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim();
        if (!STATUS_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Statut invalide.");
        }
        return normalized;
    }

    public static String requireRole(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim().toLowerCase();
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Role invalide. Valeurs autorisees: user, admin, author, client.");
        }
        return normalized;
    }

    public static int requirePositiveInt(String value, String fieldLabel) {
        try {
            int parsed = Integer.parseInt(Optional.ofNullable(value).orElse("").trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldLabel + " doit etre strictement positif.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldLabel + " invalide.");
        }
    }

    public static int requireNonNegativeInt(String value, String fieldLabel) {
        try {
            int parsed = Integer.parseInt(Optional.ofNullable(value).orElse("").trim());
            if (parsed < 0) {
                throw new IllegalArgumentException(fieldLabel + " ne peut pas etre negatif.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldLabel + " invalide.");
        }
    }

    public static double requireNonNegativeAmount(String value, String fieldLabel) {
        try {
            String normalized = Optional.ofNullable(value).orElse("").trim().replace(',', '.');
            double parsed = Double.parseDouble(normalized);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < 0) {
                throw new IllegalArgumentException(fieldLabel + " invalide.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldLabel + " invalide.");
        }
    }

    public static double requireStrictlyPositiveAmount(String value, String fieldLabel) {
        double parsed = requireNonNegativeAmount(value, fieldLabel);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldLabel + " doit etre strictement positif.");
        }
        return parsed;
    }

    public static void validateWalletAmounts(double balance, double ceiling) {
        if (ceiling <= 0) {
            throw new IllegalArgumentException("Le plafond doit etre strictement positif.");
        }
        if (balance > ceiling) {
            throw new IllegalArgumentException("Le solde ne peut pas depasser le plafond.");
        }
    }

    public static String normalizeEmail(String value) {
        return Optional.ofNullable(value).orElse("").trim().toLowerCase();
    }

    public static String normalizePhone(String value) {
        String raw = Optional.ofNullable(value).orElse("").trim();
        if (raw.isBlank()) {
            return "";
        }

        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return "+216 " + digits;
        }
        if (digits.length() == 11 && digits.startsWith("216")) {
            return "+216 " + digits.substring(3);
        }

        return raw.replaceAll("\\s+", " ").trim();
    }

    public static boolean isValidEmailFormat(String value) {
        String normalized = normalizeEmail(value);
        return !normalized.isBlank() && EMAIL_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidPhoneFormat(String value) {
        String normalized = normalizePhone(value);
        return normalized.isBlank() || PHONE_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidPhoneInput(String value) {
        return PHONE_INPUT_PATTERN.matcher(Optional.ofNullable(value).orElse("")).matches();
    }
}
