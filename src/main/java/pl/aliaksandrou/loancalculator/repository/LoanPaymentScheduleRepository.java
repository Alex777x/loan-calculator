package pl.aliaksandrou.loancalculator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.aliaksandrou.loancalculator.model.LoanPaymentSchedule;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanPaymentScheduleRepository extends JpaRepository<LoanPaymentSchedule, UUID> {
    List<LoanPaymentSchedule> findByLoanId(UUID loanId);
}
