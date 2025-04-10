package pl.aliaksandrou.loancalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@Builder
@Schema(description = "Response containing loan calculation results")
public class LoanCalculationResponse {
    @Schema(description = "Loan amount", example = "100000")
    private BigDecimal loanAmount;

    @Schema(description = "Annual interest rate in percentage", example = "5.5")
    private BigDecimal interestRate;

    @Schema(description = "Monthly payment amount", example = "567.79")
    private BigDecimal monthlyPayment;

    @Schema(description = "List of payment schedule items")
    private List<PaymentScheduleItem> payments;
}
