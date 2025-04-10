package pl.aliaksandrou.loancalculator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationRequest;
import pl.aliaksandrou.loancalculator.dto.LoanCalculationResponse;
import pl.aliaksandrou.loancalculator.service.LoanCalculationService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/loans")
@Tag(name = "Loan Calculator", description = "API for calculating loan schedules")
public class LoanCalculatorController {

    private final LoanCalculationService loanCalculationService;

    @Operation(
            summary = "Calculate loan schedule",
            description = "Calculates the loan schedule with annuity payments based on the provided parameters"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Loan schedule calculated successfully",
                    content = @Content(schema = @Schema(implementation = LoanCalculationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content
            )
    })
    @PostMapping("/calculate")
    public ResponseEntity<LoanCalculationResponse> calculateLoanSchedule(
            @Parameter(description = "Loan calculation parameters", required = true)
            @RequestBody LoanCalculationRequest request) {
        LoanCalculationResponse response = loanCalculationService.calculateLoanSchedule(request);
        return ResponseEntity.ok(response);
    }
} 
