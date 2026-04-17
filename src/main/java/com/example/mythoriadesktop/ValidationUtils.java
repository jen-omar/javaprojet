package com.example.mythoriadesktop;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ' -]{1,49}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern STATUS_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ _-]{1,29}$");
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
        String normalized = Optional.ofNullable(value).orElse("").trim().toLowerCase();
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
        String normalized = Optional.ofNullable(value).orElse("").replace(" ", "").trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Numero de telephone invalide.");
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

    public static void validateWalletAmounts(double balance, double ceiling) {
        if (ceiling > 0 && balance > ceiling) {
            throw new IllegalArgumentException("Le solde ne peut pas depasser le plafond.");
        }
    }
}
