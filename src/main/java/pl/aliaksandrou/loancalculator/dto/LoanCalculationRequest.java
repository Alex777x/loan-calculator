package pl.aliaksandrou.loancalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Schema(description = "Request for loan calculation")
public class LoanCalculationRequest {
    @Schema(description = "Loan amount", example = "100000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal loanAmount;

    @Schema(description = "Annual interest rate in percentage", example = "5.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal interestRate;

    @Schema(description = "Loan term in months", example = "360", requiredMode = Schema.RequiredMode.REQUIRED)
    private int term;
}
