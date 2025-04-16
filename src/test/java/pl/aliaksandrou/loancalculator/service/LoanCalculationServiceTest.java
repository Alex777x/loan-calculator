package pl.aliaksandrou.loancalculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.aliaksandrou.loancalculator.LoanCalculatorApplication;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationResponse;
import pl.aliaksandrou.loancalculator.model.Loan;
import pl.aliaksandrou.loancalculator.model.LoanPaymentSchedule;
import pl.aliaksandrou.loancalculator.repository.LoanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = LoanCalculatorApplication.class)
@ActiveProfiles("test")
class LoanCalculationServiceTest {

    @Configuration
    @EnableCaching
    static class CacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("loanSchedule");
        }
    }

    @MockitoBean
    private LoanRepository loanRepository;

    @Autowired
    private LoanCalculationService loanCalculationService;

    private LoanCalculationRequest request;
    private Loan existingLoan;
    private List<LoanPaymentSchedule> existingPaymentSchedules;

    @BeforeEach
    void setUp() {
        request = LoanCalculationRequest.builder()
                .loanAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("5.5"))
                .term(360)
                .build();

        existingLoan = Loan.builder()
                .id(UUID.randomUUID())
                .loanAmount(request.getLoanAmount())
                .interestRate(request.getInterestRate())
                .term(request.getTerm())
                .monthlyPayment(new BigDecimal("567.79"))
                .build();

        existingPaymentSchedules = new ArrayList<>();
        LoanPaymentSchedule schedule = LoanPaymentSchedule.builder()
                .paymentNumber(1)
                .paymentDate(LocalDate.now().plusMonths(1))
                .totalPayment(new BigDecimal("567.79"))
                .principal(new BigDecimal("110.12"))
                .interest(new BigDecimal("457.67"))
                .remainingBalance(new BigDecimal("99889.88"))
                .build();
        existingPaymentSchedules.add(schedule);
        existingLoan.setPaymentSchedule(existingPaymentSchedules);
    }

    @Test
    void calculateLoanSchedule_WithExistingLoan_ReturnsCachedResult() {
        // 1. Evict the cache to ensure we don't reuse from previous test
        loanCalculationService.evictLoanScheduleCache(request);

        // 2. Set up mock
        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.of(existingLoan));

        // 3. First call - should hit DB
        LoanCalculationResponse response1 = loanCalculationService.calculateLoanSchedule(request);
        assertNotNull(response1);

        // Verify DB call
        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        // 4. Reset mock to track 2nd call separately
        reset(loanRepository);

        // 5. Second call - should come from cache, no DB call
        LoanCalculationResponse response2 = loanCalculationService.calculateLoanSchedule(request);
        assertNotNull(response2);

        // Should NOT hit repository again
        verify(loanRepository, times(0)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        // Same results
        assertEquals(response1.getMonthlyPayment(), response2.getMonthlyPayment());
    }

    @Test
    void calculateLoanSchedule_WithDifferentParameters_DoesNotUseCache() {
        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.of(existingLoan));

        LoanCalculationResponse response1 = loanCalculationService.calculateLoanSchedule(request);
        assertNotNull(response1);

        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        LoanCalculationRequest differentRequest = LoanCalculationRequest.builder()
                .loanAmount(request.getLoanAmount())
                .interestRate(request.getInterestRate().add(BigDecimal.ONE))
                .term(request.getTerm())
                .build();

        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                differentRequest.getLoanAmount(), differentRequest.getInterestRate(), differentRequest.getTerm()))
                .thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenReturn(existingLoan);

        LoanCalculationResponse response2 = loanCalculationService.calculateLoanSchedule(differentRequest);
        assertNotNull(response2);

        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                differentRequest.getLoanAmount(), differentRequest.getInterestRate(), differentRequest.getTerm());
    }

    @Test
    void evictLoanScheduleCache_RemovesCachedResult() {
        // Make sure the cache is clean before this test
        loanCalculationService.evictLoanScheduleCache(request);

        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.of(existingLoan));

        // First call - should populate cache
        LoanCalculationResponse response1 = loanCalculationService.calculateLoanSchedule(request);
        assertNotNull(response1);

        // Should be 1 call to DB
        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());

        // Evict cache
        loanCalculationService.evictLoanScheduleCache(request);

        // Reset mock to track second interaction separately
        reset(loanRepository);

        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.of(existingLoan));

        // Call again — should hit DB again due to cache eviction
        LoanCalculationResponse response2 = loanCalculationService.calculateLoanSchedule(request);
        assertNotNull(response2);

        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());
    }

    @Test
    void calculateLoanSchedule_WithNewLoan_CreatesAndSavesNewLoan() {
        loanCalculationService.evictLoanScheduleCache(request); // 💥 clear cache

        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenReturn(existingLoan);

        LoanCalculationResponse response = loanCalculationService.calculateLoanSchedule(request);

        assertNotNull(response);
        assertEquals(request.getLoanAmount(), response.getLoanAmount());
        assertEquals(request.getInterestRate(), response.getInterestRate());

        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void calculateMonthlyPayment_WithValidInputs_ReturnsCorrectValue() {
        BigDecimal monthlyPayment = loanCalculationService.calculateMonthlyPayment(request);

        assertNotNull(monthlyPayment);
        assertEquals(0, monthlyPayment.compareTo(new BigDecimal("567.79")));
    }

    @Test
    void calculateMonthlyPayment_WithZeroInterest_ReturnsSimpleDivision() {
        request = LoanCalculationRequest.builder()
                .loanAmount(new BigDecimal("120000"))
                .interestRate(BigDecimal.ZERO)
                .term(360)
                .build();

        BigDecimal monthlyPayment = loanCalculationService.calculateMonthlyPayment(request);

        assertNotNull(monthlyPayment);
        assertEquals(0, monthlyPayment.compareTo(new BigDecimal("333.33")));
    }

    @Test
    void generatePaymentSchedule_WithValidInputs_ReturnsCorrectSchedule() {
        List<LoanPaymentSchedule> schedules = loanCalculationService.generatePaymentSchedule(existingLoan, existingLoan.getMonthlyPayment());

        assertNotNull(schedules);
        assertEquals(existingLoan.getTerm(), schedules.size());

        LoanPaymentSchedule firstPayment = schedules.get(0);
        assertEquals(1, firstPayment.getPaymentNumber());
        assertNotNull(firstPayment.getPaymentDate());
        assertEquals(0, firstPayment.getTotalPayment().compareTo(existingLoan.getMonthlyPayment()));
        assertTrue(firstPayment.getPrincipal().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(firstPayment.getInterest().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(firstPayment.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0);

        LoanPaymentSchedule lastPayment = schedules.get(schedules.size() - 1);
        assertEquals(existingLoan.getTerm(), lastPayment.getPaymentNumber());
        assertEquals(0, lastPayment.getRemainingBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateLoanSchedule_WithInvalidLoanAmount_ThrowsException() {
        request = LoanCalculationRequest.builder()
                .loanAmount(BigDecimal.ZERO)
                .interestRate(new BigDecimal("5.5"))
                .term(360)
                .build();

        assertThrows(IllegalArgumentException.class, () -> loanCalculationService.calculateLoanSchedule(request));
    }

    @Test
    void calculateLoanSchedule_WithInvalidInterestRate_ThrowsException() {
        request = LoanCalculationRequest.builder()
                .loanAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("-1"))
                .term(360)
                .build();

        assertThrows(IllegalArgumentException.class, () -> loanCalculationService.calculateLoanSchedule(request));
    }

    @Test
    void calculateLoanSchedule_WithInvalidTerm_ThrowsException() {
        request = LoanCalculationRequest.builder()
                .loanAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("5.5"))
                .term(0)
                .build();

        assertThrows(IllegalArgumentException.class, () -> loanCalculationService.calculateLoanSchedule(request));
    }
}
