package pl.aliaksandrou.loancalculator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.aliaksandrou.loancalculator.model.Loan;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Optional<Loan> findByLoanAmountAndInterestRateAndTerm(BigDecimal loanAmount, BigDecimal interestRate, Integer term);
}
