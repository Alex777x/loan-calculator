package pl.aliaksandrou.loancalculator.service;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationResponse;
import pl.aliaksandrou.loancalculator.dto.PaymentScheduleItem;
import pl.aliaksandrou.loancalculator.model.Loan;
import pl.aliaksandrou.loancalculator.model.LoanPaymentSchedule;
import pl.aliaksandrou.loancalculator.repository.LoanPaymentScheduleRepository;
import pl.aliaksandrou.loancalculator.repository.LoanRepository;
import pl.aliaksandrou.loancalculator.validator.LoanCalculationValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LoanCalculationService {

    private final LoanRepository loanRepository;
    private final LoanPaymentScheduleRepository paymentScheduleRepository;

    @Cacheable(value = "loanSchedule", key = "#request.loanAmount + '-' + #request.interestRate + '-' + #request.term")
    public LoanCalculationResponse calculateLoanSchedule(LoanCalculationRequest request) {
        LoanCalculationValidator.validateRequest(request);

        Optional<Loan> existingLoanOpt = loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        Loan loan;
        if (existingLoanOpt.isPresent()) {
            loan = existingLoanOpt.get();
        } else {
            loan = Loan.builder()
                    .loanAmount(request.getLoanAmount())
                    .interestRate(request.getInterestRate())
                    .term(request.getTerm())
                    .build();
            loan = loanRepository.save(loan);

            BigDecimal monthlyPayment = calculateMonthlyPayment(request);
            List<LoanPaymentSchedule> paymentSchedules = generatePaymentSchedule(loan, monthlyPayment);
            paymentScheduleRepository.saveAll(paymentSchedules);
        }

        BigDecimal monthlyPayment = calculateMonthlyPayment(request);

        List<LoanPaymentSchedule> schedules = paymentScheduleRepository.findByLoanId(loan.getId());
        if (schedules == null || schedules.isEmpty()) {
            schedules = generatePaymentSchedule(loan, monthlyPayment);
        }

        return LoanCalculationResponse.builder()
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .monthlyPayment(monthlyPayment)
                .payments(generatePaymentScheduleResponse(schedules))
                .build();
    }

    @CacheEvict(value = "loanSchedule", key = "#request.loanAmount + '-' + #request.interestRate + '-' + #request.term")
    public void evictLoanScheduleCache(LoanCalculationRequest request) {
        // Method to evict cache
    }

    private BigDecimal calculateMonthlyPayment(LoanCalculationRequest request) {
        BigDecimal loanAmount = request.getLoanAmount();
        BigDecimal interestRate = request.getInterestRate();
        int termMonths = request.getTerm();

        // Handle 0% interest rate case
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }

        // Convert annual interest rate to monthly rate (divide by 100 to get decimal, then by 12 for monthly)
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        // Calculate (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(termMonths);

        // Calculate P * r * (1 + r)^n / ((1 + r)^n - 1)
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private List<LoanPaymentSchedule> generatePaymentSchedule(Loan loan, BigDecimal monthlyPayment) {
        List<LoanPaymentSchedule> paymentSchedules = new ArrayList<>();
        BigDecimal remainingBalance = loan.getLoanAmount();
        BigDecimal monthlyRate = loan.getInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        for (int month = 1; month <= loan.getTerm(); month++) {
            LoanPaymentSchedule schedule = LoanPaymentSchedule.builder()
                    .loan(loan)
                    .paymentNumber(month)
                    .paymentDate(LocalDate.now().plusMonths(month))
                    .build();

            // Calculate interest for this payment
            BigDecimal interest = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            // Calculate principal for this payment
            BigDecimal principal;

            // Handle final payment
            if (month == loan.getTerm()) {
                principal = remainingBalance;
                // Adjust payment amount for final payment to handle rounding issues
                schedule.setTotalPayment(principal.add(interest));
            } else {
                principal = monthlyPayment.subtract(interest).setScale(2, RoundingMode.HALF_UP);
                schedule.setTotalPayment(monthlyPayment);
            }

            // Calculate new remaining balance
            BigDecimal newRemainingBalance = remainingBalance.subtract(principal).setScale(2, RoundingMode.HALF_UP);

            // Ensure the last payment brings the balance to exactly zero
            if (month == loan.getTerm()) {
                newRemainingBalance = BigDecimal.ZERO;
            }

            schedule.setInterest(interest);
            schedule.setPrincipal(principal);
            schedule.setRemainingBalance(newRemainingBalance);

            remainingBalance = newRemainingBalance;
            paymentSchedules.add(schedule);
        }
        return paymentSchedules;
    }

    private List<PaymentScheduleItem> generatePaymentScheduleResponse(List<LoanPaymentSchedule> paymentSchedules) {
        return paymentSchedules.stream().map(schedule -> PaymentScheduleItem.builder()
                .number(schedule.getPaymentNumber())
                .date(schedule.getPaymentDate().toString())
                .totalPayment(schedule.getTotalPayment())
                .interest(schedule.getInterest())
                .principal(schedule.getPrincipal())
                .remainingBalance(schedule.getRemainingBalance())
                .build()).toList();
    }
}
