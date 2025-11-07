package com.taskgoapp.taskgo.validation

import com.taskgoapp.taskgo.core.validation.Validators
import org.junit.Test
import org.junit.Assert.*

class ValidatorsTest {

    @Test
    fun `isValidEmail should return true for valid emails`() {
        assertTrue(Validators.isValidEmail("test@example.com"))
        assertTrue(Validators.isValidEmail("user.name@domain.co.uk"))
        assertTrue(Validators.isValidEmail("test+tag@example.org"))
    }

    @Test
    fun `isValidEmail should return false for invalid emails`() {
        assertFalse(Validators.isValidEmail("invalid-email"))
        assertFalse(Validators.isValidEmail("@example.com"))
        assertFalse(Validators.isValidEmail("test@"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun `isValidPhone should return true for valid Brazilian phones`() {
        assertTrue(Validators.isValidPhone("(11) 99999-9999"))
        assertTrue(Validators.isValidPhone("11999999999"))
        assertTrue(Validators.isValidPhone("(11) 8888-8888"))
        assertTrue(Validators.isValidPhone("1188888888"))
    }

    @Test
    fun `isValidPhone should return false for invalid phones`() {
        assertFalse(Validators.isValidPhone("123"))
        assertFalse(Validators.isValidPhone("123456789012"))
        assertFalse(Validators.isValidPhone(""))
    }

    @Test
    fun `isValidCep should return true for valid Brazilian CEPs`() {
        assertTrue(Validators.isValidCep("01234-567"))
        assertTrue(Validators.isValidCep("01234567"))
    }

    @Test
    fun `isValidCep should return false for invalid CEPs`() {
        assertFalse(Validators.isValidCep("123"))
        assertFalse(Validators.isValidCep("123456789"))
        assertFalse(Validators.isValidCep(""))
    }

    @Test
    fun `isValidCreditCard should return true for valid card numbers`() {
        // Test with a valid Luhn number
        assertTrue(Validators.isValidCreditCard("4532015112830366"))
        assertTrue(Validators.isValidCreditCard("4532 0151 1283 0366"))
    }

    @Test
    fun `isValidCreditCard should return false for invalid card numbers`() {
        assertFalse(Validators.isValidCreditCard("1234567890123456"))
        assertFalse(Validators.isValidCreditCard("123"))
        assertFalse(Validators.isValidCreditCard(""))
    }

    @Test
    fun `isValidCvc should return true for valid CVCs`() {
        assertTrue(Validators.isValidCvc("123"))
        assertTrue(Validators.isValidCvc("000"))
        assertTrue(Validators.isValidCvc("999"))
    }

    @Test
    fun `isValidCvc should return false for invalid CVCs`() {
        assertFalse(Validators.isValidCvc("12"))
        assertFalse(Validators.isValidCvc("1234"))
        assertFalse(Validators.isValidCvc("abc"))
        assertFalse(Validators.isValidCvc(""))
    }

    @Test
    fun `isValidExpiryDate should return true for valid future dates`() {
        assertTrue(Validators.isValidExpiryDate("12/25"))
        assertTrue(Validators.isValidExpiryDate("01/26"))
    }

    @Test
    fun `isValidExpiryDate should return false for invalid dates`() {
        assertFalse(Validators.isValidExpiryDate("13/25"))
        assertFalse(Validators.isValidExpiryDate("00/25"))
        assertFalse(Validators.isValidExpiryDate("12/23"))
        assertFalse(Validators.isValidExpiryDate("invalid"))
        assertFalse(Validators.isValidExpiryDate(""))
    }

    @Test
    fun `isValidName should return true for valid names`() {
        assertTrue(Validators.isValidName("João Silva"))
        assertTrue(Validators.isValidName("Maria José"))
        assertTrue(Validators.isValidName("José"))
    }

    @Test
    fun `isValidName should return false for invalid names`() {
        assertFalse(Validators.isValidName("J"))
        assertFalse(Validators.isValidName("João123"))
        assertFalse(Validators.isValidName(""))
    }

    @Test
    fun `isValidPassword should return true for valid passwords`() {
        assertTrue(Validators.isValidPassword("123456"))
        assertTrue(Validators.isValidPassword("password123"))
    }

    @Test
    fun `isValidPassword should return false for invalid passwords`() {
        assertFalse(Validators.isValidPassword("12345"))
        assertFalse(Validators.isValidPassword(""))
    }

    @Test
    fun `isValidPrice should return true for valid prices`() {
        assertTrue(Validators.isValidPrice("10.50"))
        assertTrue(Validators.isValidPrice("10,50"))
        assertTrue(Validators.isValidPrice("100"))
    }

    @Test
    fun `isValidPrice should return false for invalid prices`() {
        assertFalse(Validators.isValidPrice("0"))
        assertFalse(Validators.isValidPrice("-10"))
        assertFalse(Validators.isValidPrice("invalid"))
        assertFalse(Validators.isValidPrice(""))
    }

    @Test
    fun `isValidDescription should return true for valid descriptions`() {
        assertTrue(Validators.isValidDescription("This is a valid description with more than 10 characters"))
    }

    @Test
    fun `isValidDescription should return false for invalid descriptions`() {
        assertFalse(Validators.isValidDescription("Short"))
        assertFalse(Validators.isValidDescription(""))
    }
}
