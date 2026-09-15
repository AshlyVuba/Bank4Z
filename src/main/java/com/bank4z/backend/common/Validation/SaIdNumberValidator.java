package com.bank4z.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validates a South African ID number:
 *  - exactly 13 digits
 *  - first 6 digits form a real date of birth (YYMMDD)
 *  - passes the Luhn checksum (the standard SA ID check digit algorithm)
 *
 * Note: this checks the number is well-formed, not that it belongs to a real person.
 */
public class SaIdNumberValidator implements ConstraintValidator<ValidSaId, String> {

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    @Override
    public boolean isValid(String idNumber, ConstraintValidatorContext context) {
        if (idNumber == null || !idNumber.matches("\\d{13}")) {
            return false;
        }

        if (!hasValidDateOfBirth(idNumber.substring(0, 6))) {
            return false;
        }

        return passesLuhnCheck(idNumber);
    }

    private boolean hasValidDateOfBirth(String yyMMdd) {
        try {
            LocalDate.parse(yyMMdd, DOB_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean passesLuhnCheck(String idNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = idNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(idNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}