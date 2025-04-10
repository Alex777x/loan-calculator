package pl.aliaksandrou.loancalculator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_payment_schedule")
public class LoanPaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private Integer paymentNumber;
    private LocalDate paymentDate;
    private BigDecimal totalPayment;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal remainingBalance;
    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
}
