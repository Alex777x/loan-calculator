package pl.aliaksandrou.loancalculator.validator;

import lombok.experimental.UtilityClass;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;

import java.math.BigDecimal;

@UtilityClass
public class LoanCalculationValidator {

    public void validateRequest(LoanCalculationRequest request) {
        if (request.getLoanAmount() == null || request.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (request.getInterestRate() == null || request.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must be greater than or equal to zero");
        }
        if (request.getTerm() <= 0) {
            throw new IllegalArgumentException("Term must be greater than zero");
        }
    }
}
