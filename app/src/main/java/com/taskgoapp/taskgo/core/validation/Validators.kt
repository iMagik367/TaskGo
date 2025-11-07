package com.taskgoapp.taskgo.core.validation

import java.util.regex.Pattern

object Validators {
    
    // Email validation
    fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        )
        return emailPattern.matcher(email).matches()
    }
    
    // Phone validation (Brazilian format)
    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return cleanPhone.length >= 10 && cleanPhone.length <= 11
    }
    
    // CEP validation (Brazilian postal code)
    fun isValidCep(cep: String): Boolean {
        val cleanCep = cep.replace(Regex("[^0-9]"), "")
        return cleanCep.length == 8
    }
    
    // Credit card number validation (Luhn algorithm)
    fun isValidCreditCard(cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        if (cleanNumber.length < 13 || cleanNumber.length > 19) return false
        
        var sum = 0
        var alternate = false
        
        for (i in cleanNumber.length - 1 downTo 0) {
            var n = cleanNumber[i].toString().toInt()
            
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    // CVC validation
    fun isValidCvc(cvc: String): Boolean {
        val cleanCvc = cvc.replace(Regex("[^0-9]"), "")
        return cleanCvc.length == 3
    }
    
    // Expiry date validation (MM/YY format)
    fun isValidExpiryDate(expiryDate: String): Boolean {
        val pattern = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$")
        val matcher = pattern.matcher(expiryDate)
        
        if (!matcher.matches()) return false
        
        val month = matcher.group(1)!!.toInt()
        val year = matcher.group(2)!!.toInt()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        return if (year > currentYear) {
            true
        } else if (year == currentYear) {
            month >= currentMonth
        } else {
            false
        }
    }
    
    // Name validation
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2 && name.matches(Regex("^[a-zA-ZÀ-ÿ\\s]+$"))
    }
    
    // Password validation
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    // Price validation
    fun isValidPrice(price: String): Boolean {
        return try {
            val value = price.replace(",", ".").toDouble()
            value > 0
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    // Description validation (minimum 10 characters)
    fun isValidDescription(description: String): Boolean {
        return description.trim().length >= 10
    }
    
    // Address validation
    fun isValidAddress(address: String): Boolean {
        return address.trim().length >= 5
    }
    
    // City validation
    fun isValidCity(city: String): Boolean {
        return city.trim().length >= 2
    }
    
    // State validation (2 characters)
    fun isValidState(state: String): Boolean {
        return state.trim().length == 2
    }
}

// Validation result wrapper
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

// Form validation helper
class FormValidator {
    private val errors = mutableMapOf<String, String>()
    
    fun validate(fieldName: String, value: String, validator: (String) -> Boolean, errorMessage: String): FormValidator {
        if (!validator(value)) {
            errors[fieldName] = errorMessage
        } else {
            errors.remove(fieldName)
        }
        return this
    }
    
    fun isValid(): Boolean = errors.isEmpty()
    
    fun getErrors(): Map<String, String> = errors.toMap()
    
    fun getError(fieldName: String): String? = errors[fieldName]
}
