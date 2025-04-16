package pl.aliaksandrou.loancalculator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationResponse;
import pl.aliaksandrou.loancalculator.dto.PaymentScheduleItem;
import pl.aliaksandrou.loancalculator.model.Loan;
import pl.aliaksandrou.loancalculator.model.LoanPaymentSchedule;
import pl.aliaksandrou.loancalculator.repository.LoanRepository;
import pl.aliaksandrou.loancalculator.validator.LoanCalculationValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class LoanCalculationService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENTAGE_DIVISOR = BigDecimal.valueOf(100);
    private static final int SCALE = 10;
    private static final int RESULT_SCALE = 2;

    private final LoanRepository loanRepository;

    @Cacheable(value = "loanSchedule", key = "#request.loanAmount + '-' + #request.interestRate + '-' + #request.term")
    public LoanCalculationResponse calculateLoanSchedule(LoanCalculationRequest request) {
        log.info("Calculating loan schedule for request: {}", request);
        LoanCalculationValidator.validateRequest(request);

        Optional<Loan> existingLoanOpt = loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        Loan loan = existingLoanOpt.orElseGet(() -> {
            BigDecimal monthlyPayment = calculateMonthlyPayment(request);
            return createNewLoan(request, monthlyPayment);
        });

        return buildResponse(loan, loan.getMonthlyPayment());
    }

    private Loan createNewLoan(LoanCalculationRequest request, BigDecimal monthlyPayment) {
        log.debug("Creating new loan for request: {}", request);

        Loan loan = Loan.builder()
                .loanAmount(request.getLoanAmount())
                .interestRate(request.getInterestRate())
                .term(request.getTerm())
                .monthlyPayment(monthlyPayment)
                .build();

        List<LoanPaymentSchedule> paymentSchedules = generatePaymentSchedule(loan, monthlyPayment);
        loan.setPaymentSchedule(paymentSchedules);

        return loanRepository.save(loan);
    }

    private LoanCalculationResponse buildResponse(Loan loan, BigDecimal monthlyPayment) {
        return LoanCalculationResponse.builder()
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .monthlyPayment(monthlyPayment)
                .payments(generatePaymentScheduleResponse(loan.getPaymentSchedule()))
                .build();
    }

    @CacheEvict(value = "loanSchedule", key = "#request.loanAmount + '-' + #request.interestRate + '-' + #request.term")
    public void evictLoanScheduleCache(LoanCalculationRequest request) {
        log.debug("Evicting cache for request: {}", request);
    }

    /**
     * Calculates monthly payment using the annuity formula.
     * For zero interest rate, uses simple division.
     *
     * @param request Loan calculation parameters
     * @return Monthly payment amount
     */
    public BigDecimal calculateMonthlyPayment(LoanCalculationRequest request) {
        BigDecimal loanAmount = request.getLoanAmount();
        BigDecimal interestRate = request.getInterestRate();
        int termMonths = request.getTerm();

        // Handle the case where the interest rate is 0% (simple division of loan amount by number of months)
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            // For 0% interest rate, we simply divide the loan amount by the term (number of months)
            return loanAmount.divide(BigDecimal.valueOf(termMonths), RESULT_SCALE, RoundingMode.HALF_UP);
        }

        // Calculate the monthly interest rate from the annual rate
        BigDecimal monthlyRate = calculateMonthlyRate(interestRate);

        // Formula step: (1 + r), where r is the monthly interest rate
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);

        // Formula step: (1 + r)^n, where n is the term in months (compounding interest factor)
        BigDecimal power = onePlusR.pow(termMonths);

        // Formula step: P * r * (1 + r)^n
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(power);

        // Formula step: (1 + r)^n - 1 (the denominator of the formula)
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        // Final formula: P * r * (1 + r)^n / ((1 + r)^n - 1)
        return numerator.divide(denominator, RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyRate(BigDecimal annualRate) {
        return annualRate.divide(PERCENTAGE_DIVISOR, SCALE, RoundingMode.HALF_UP)
                .divide(MONTHS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Generates payment schedule for the loan.
     * Each payment includes principal, interest, and remaining balance.
     *
     * @param loan           Loan details
     * @param monthlyPayment Calculated monthly payment
     * @return List of payment schedules
     */
    public List<LoanPaymentSchedule> generatePaymentSchedule(Loan loan, BigDecimal monthlyPayment) {
        List<LoanPaymentSchedule> paymentSchedules = new ArrayList<>();
        BigDecimal remainingBalance = loan.getLoanAmount();
        BigDecimal monthlyRate = calculateMonthlyRate(loan.getInterestRate());

        for (int month = 1; month <= loan.getTerm(); month++) {
            LoanPaymentSchedule schedule = createPaymentSchedule(
                    loan, month, remainingBalance, monthlyRate, monthlyPayment
            );
            remainingBalance = schedule.getRemainingBalance();
            paymentSchedules.add(schedule);
        }
        return paymentSchedules;
    }

    private LoanPaymentSchedule createPaymentSchedule(Loan loan, int month, BigDecimal remainingBalance,
                                                      BigDecimal monthlyRate, BigDecimal monthlyPayment) {
        BigDecimal interest = remainingBalance.multiply(monthlyRate).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
        BigDecimal principal = calculatePrincipal(month, loan.getTerm(), remainingBalance, monthlyPayment, interest);
        BigDecimal newRemainingBalance = calculateNewBalance(month, loan.getTerm(), remainingBalance, principal);

        return LoanPaymentSchedule.builder()
                .loan(loan)
                .paymentNumber(month)
                .paymentDate(LocalDate.now().plusMonths(month))
                .interest(interest)
                .principal(principal)
                .remainingBalance(newRemainingBalance)
                .totalPayment(month == loan.getTerm() ? principal.add(interest) : monthlyPayment)
                .build();
    }

    private BigDecimal calculatePrincipal(int month, int term, BigDecimal remainingBalance,
                                          BigDecimal monthlyPayment, BigDecimal interest) {
        return (month == term)
                ? remainingBalance
                : monthlyPayment.subtract(interest).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNewBalance(int month, int term, BigDecimal remainingBalance, BigDecimal principal) {
        BigDecimal newBalance = remainingBalance.subtract(principal).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
        return (month == term) ? BigDecimal.ZERO : newBalance;
    }

    private List<PaymentScheduleItem> generatePaymentScheduleResponse(List<LoanPaymentSchedule> paymentSchedules) {
        return paymentSchedules.stream()
                .map(schedule -> PaymentScheduleItem.builder()
                        .number(schedule.getPaymentNumber())
                        .date(schedule.getPaymentDate().toString())
                        .totalPayment(schedule.getTotalPayment())
                        .interest(schedule.getInterest())
                        .principal(schedule.getPrincipal())
                        .remainingBalance(schedule.getRemainingBalance())
                        .build())
                .toList();
    }
}
