package pl.aliaksandrou.loancalculator.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan", uniqueConstraints = {
        @UniqueConstraint(name = "unique_loan", columnNames = {"loan_amount", "interest_rate", "term"})
})
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "loan_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "term", nullable = false)
    private Integer term;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentNumber ASC")
    private List<LoanPaymentSchedule> paymentSchedule = new ArrayList<>();

    @Column(name = "monthly_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
}
