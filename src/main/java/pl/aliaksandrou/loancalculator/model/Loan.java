package pl.aliaksandrou.loancalculator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer term;
    private LocalDate createdAt = LocalDate.now();
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanPaymentSchedule> paymentSchedule;
}
