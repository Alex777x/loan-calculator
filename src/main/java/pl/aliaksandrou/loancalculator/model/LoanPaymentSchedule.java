package pl.aliaksandrou.loancalculator.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "total_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal interest;

    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
}
