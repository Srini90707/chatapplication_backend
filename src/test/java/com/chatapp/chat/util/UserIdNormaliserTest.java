package com.chatapp.chat.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class UserIdNormaliserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "+91 98765 43210",
            "919876543210",
            "9876543210",
            "+919876543210"
    })
    @DisplayName("Non-null phone numbers should normalise to 9876543210")
    void shouldNormalisePhoneNumbersTo10Digits(String rawId) {
        String result = UserIdNormaliser.normalise(rawId);
        assertEquals("9876543210", result);
    }

    @Test
    @DisplayName("null input should return null")
    void shouldReturnNullForNullInput() {
        assertNull(UserIdNormaliser.normalise(null));
    }

    @Test
    @DisplayName("empty input should return empty string")
    void shouldReturnEmptyStringForEmptyInput() {
        assertEquals("", UserIdNormaliser.normalise(""));
    }

    @Test
    @DisplayName("private constructor throws AssertionError on instantiation")
    void privateConstructorShouldThrowAssertionError() throws NoSuchMethodException {
        Constructor<UserIdNormaliser> constructor = UserIdNormaliser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(AssertionError.class, exception.getCause());
    }
}
