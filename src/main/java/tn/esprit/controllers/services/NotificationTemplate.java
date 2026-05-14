package tn.esprit.controllers.services;

import tn.esprit.Models.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class NotificationTemplate {
    public static final String LOGIN_ALERT = "LOGIN_ALERT";
    public static final String ROLE_CHANGE_ALERT = "ROLE_CHANGE_ALERT";
    public static final String PASSWORD_CHANGE_ALERT = "PASSWORD_CHANGE_ALERT";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private NotificationTemplate() {
    }

    public static EmailMessage loginAlert(User user, LocalDateTime loginTime) {
        String displayName = displayName(user);
        String formattedTime = loginTime.format(DATE_TIME_FORMATTER);
        String message = """
                Hello %s,

                Your Mythoria account was successfully accessed on %s.

                If this was you, no action is required.
                If you do not recognize this login, please contact Mythoria support immediately.

                Mythoria Security
                """.formatted(displayName, formattedTime);
        return new EmailMessage("Mythoria security alert: new login", message);
    }

    public static EmailMessage roleChangeAlert(User user, String oldRole, String newRole) {
        String message = """
                Hello %s,

                Your Mythoria account role has been changed.

                Old role: %s
                New role: %s

                If you did not expect this change, please contact Mythoria support.

                Mythoria Security
                """.formatted(displayName(user), clean(oldRole), clean(newRole));
        return new EmailMessage("Mythoria security alert: role changed", message);
    }

    public static EmailMessage passwordChangeAlert(User user) {
        String message = """
                Hello %s,

                Your Mythoria account password was changed or reset.

                If you performed this action, no action is required.
                If you did not perform this action, please contact Mythoria support immediately.

                Mythoria Security
                """.formatted(displayName(user));
        return new EmailMessage("Mythoria security alert: password changed", message);
    }

    private static String displayName(User user) {
        if (user == null) {
            return "Mythoria user";
        }
        String fullName = (user.firstName() + " " + user.lastName()).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!user.displayName().isBlank()) {
            return user.displayName();
        }
        return user.username();
    }

    private static String clean(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }

    public record EmailMessage(String subject, String message) {
    }
}
