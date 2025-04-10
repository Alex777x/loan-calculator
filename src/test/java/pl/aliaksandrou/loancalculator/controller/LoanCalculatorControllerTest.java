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
                .andExpect(jsonPath("$.monthlyPayment").exists())
                .andExpect(jsonPath("$.payments").isArray());
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
    }
} 