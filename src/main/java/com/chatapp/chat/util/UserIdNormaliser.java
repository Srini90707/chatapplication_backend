package com.chatapp.chat.util;

public final class UserIdNormaliser {

    private UserIdNormaliser() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String normalise(String rawId) {
        if (rawId == null) {
            return null;
        }
        String digits = rawId.replaceAll("\\D", "");
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }
}
