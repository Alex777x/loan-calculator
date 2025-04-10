package pl.aliaksandrou.loancalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Schema(description = "Payment schedule item details")
public class PaymentScheduleItem {
    @Schema(description = "Payment number", example = "1")
    private int number;

    @Schema(description = "Payment date in YYYY-MM-DD format", example = "2024-05-01")
    private String date;

    @Schema(description = "Total payment amount", example = "567.79")
    private BigDecimal totalPayment;

    @Schema(description = "Interest portion of the payment", example = "457.67")
    private BigDecimal interest;

    @Schema(description = "Principal portion of the payment", example = "110.12")
    private BigDecimal principal;

    @Schema(description = "Remaining loan balance after payment", example = "99889.88")
    private BigDecimal remainingBalance;
}
