# Task Requirements

Build a SPRING BOOT application to calculate loan schedule with annuity payments by inputting loan amount, interest rate
and term.

-----------------------------------------------------------------------------------------------------------------------

# Getting Started

### Prerequisites

• Java 24   
• Docker    
• Maven

### Installation

Clone the repository:

```bash
git clone https://github.com/Alex777x/loan-calculator.git
```

Navigate to the application folder:

```bash
cd loan-calculator
```

Run Maven command:

```bash
mvn clean package
```

### Running the application

To run the application, execute the following command:

```bash
docker compose up --build
```

The application will be available at `http://localhost:8080`.

To stop the application, execute the following command:

```bash
docker compose down -v
```

### Tests

Tests are written using JUnit 5 and Mockito. To run the tests, execute the following command:

```bash
mvn test -Dspring.profiles.active=test -Dspring.datasource.url=jdbc:h2:mem:testdb -Dspring.datasource.username=sa -Dspring.datasource.password=
```

Run queries from the **loan-calculator-requests-test.http** file to test the application endpoints.

### API Documentation

The API documentation is available at `http://localhost:8080/swagger-ui/index.html`.    
