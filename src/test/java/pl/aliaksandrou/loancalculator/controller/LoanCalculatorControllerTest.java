package pl.aliaksandrou.loancalculator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calculateLoanSchedule_ValidRequest_ReturnsOk() throws Exception {
        String requestJson = """
            {
                "loanAmount": 100000,
                "interestRate": 5.5,
                "term": 360
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").value(100000))
                .andExpect(jsonPath("$.interestRate").value(5.5))
                .andExpect(jsonPath("$.monthlyPayment").value(567.79))
                .andExpect(jsonPath("$.payments").isArray())
                .andExpect(jsonPath("$.payments.length()").value(360))
                .andExpect(jsonPath("$.payments[0].number").value(1))
                .andExpect(jsonPath("$.payments[0].totalPayment").value(567.79))
                .andExpect(jsonPath("$.payments[0].principal").exists())
                .andExpect(jsonPath("$.payments[0].interest").exists())
                .andExpect(jsonPath("$.payments[0].remainingBalance").exists())
                .andExpect(jsonPath("$.payments[359].number").value(360))
                .andExpect(jsonPath("$.payments[359].remainingBalance").value(0.00));
    }

    @Test
    void calculateLoanSchedule_WithZeroInterest_ReturnsCorrectSchedule() throws Exception {
        String requestJson = """
            {
                "loanAmount": 120000,
                "interestRate": 0,
                "term": 360
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(333.33))
                .andExpect(jsonPath("$.payments[0].interest").value(0.00))
                .andExpect(jsonPath("$.payments[0].principal").value(333.33));
    }

    @Test
    void calculateLoanSchedule_InvalidLoanAmount_ReturnsBadRequest() throws Exception {
        String requestJson = """
            {
                "loanAmount": 0,
                "interestRate": 5.5,
                "term": 360
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Loan amount must be greater than zero"));
    }

    @Test
    void calculateLoanSchedule_InvalidInterestRate_ReturnsBadRequest() throws Exception {
        String requestJson = """
            {
                "loanAmount": 100000,
                "interestRate": -1,
                "term": 360
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interest rate must be greater than or equal to zero"));
    }

    @Test
    void calculateLoanSchedule_InvalidTerm_ReturnsBadRequest() throws Exception {
        String requestJson = """
            {
                "loanAmount": 100000,
                "interestRate": 5.5,
                "term": 0
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Term must be greater than zero"));
    }

    @Test
    void calculateLoanSchedule_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        String requestJson = """
            {
                "loanAmount": 100000,
                "term": 360
            }
            """;

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interest rate must be greater than or equal to zero"));
    }

    @Test
    void calculateLoanSchedule_InvalidJson_ReturnsBadRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateLoanSchedule_WithSameParameters_UsesCache() throws Exception {
        String requestJson = """
            {
                "loanAmount": 100000,
                "interestRate": 5.5,
                "term": 360
            }
            """;

        // First call
        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(567.79));

        // Second call with same parameters
        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(567.79));
    }

    @Test
    void calculateLoanSchedule_WithDifferentParameters_DoesNotUseCache() throws Exception {
        String requestJson1 = """
            {
                "loanAmount": 100000,
                "interestRate": 5.5,
                "term": 360
            }
            """;

        String requestJson2 = """
            {
                "loanAmount": 100000,
                "interestRate": 6.5,
                "term": 360
            }
            """;

        // First call
        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(567.79));

        // Second call with different interest rate
        mockMvc.perform(post("/api/loans/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(632.07));
    }
} 