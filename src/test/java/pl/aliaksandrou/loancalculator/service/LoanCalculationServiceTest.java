package pl.aliaksandrou.loancalculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationResponse;
import pl.aliaksandrou.loancalculator.model.Loan;
import pl.aliaksandrou.loancalculator.model.LoanPaymentSchedule;
import pl.aliaksandrou.loancalculator.repository.LoanPaymentScheduleRepository;
import pl.aliaksandrou.loancalculator.repository.LoanRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class LoanCalculationServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanPaymentScheduleRepository paymentScheduleRepository;

    @InjectMocks
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
        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.of(existingLoan));
        when(paymentScheduleRepository.findByLoanId(existingLoan.getId()))
                .thenReturn(existingPaymentSchedules);

        LoanCalculationResponse response = loanCalculationService.calculateLoanSchedule(request);

        assertNotNull(response);
        assertEquals(request.getLoanAmount(), response.getLoanAmount());
        assertEquals(request.getInterestRate(), response.getInterestRate());
        assertEquals(1, response.getPayments().size());
        verify(loanRepository, times(1)).findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm());
        verify(paymentScheduleRepository, never()).saveAll(any());
    }

    @Test
    void calculateLoanSchedule_WithNewLoan_CreatesAndSavesNewLoan() {
        when(loanRepository.findByLoanAmountAndInterestRateAndTerm(
                request.getLoanAmount(), request.getInterestRate(), request.getTerm()))
                .thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenReturn(existingLoan);
        when(paymentScheduleRepository.saveAll(any())).thenReturn(existingPaymentSchedules);

        LoanCalculationResponse response = loanCalculationService.calculateLoanSchedule(request);

        assertNotNull(response);
        assertEquals(request.getLoanAmount(), response.getLoanAmount());
        assertEquals(request.getInterestRate(), response.getInterestRate());
        verify(loanRepository, times(1)).save(any(Loan.class));
        verify(paymentScheduleRepository, times(1)).saveAll(any());
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
