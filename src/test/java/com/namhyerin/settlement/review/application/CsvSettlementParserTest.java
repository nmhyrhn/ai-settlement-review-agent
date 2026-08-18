package com.namhyerin.settlement.review.application;

import com.namhyerin.settlement.review.exception.InvalidCsvException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvSettlementParserTest {
    private final CsvSettlementParser parser = new CsvSettlementParser();

    @Test
    void parsesCsvIncludingQuotedMerchant() {
        var file = csv("transactionId,transactionDate,merchant,amount,receiptNumber\n"
                + "T-001,2026-08-18,\"ABC, 상사\",1250000,\n");

        var result = parser.parse(file);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().merchant()).isEqualTo("ABC, 상사");
    }

    @Test
    void rejectsDuplicateTransactionId() {
        var file = csv("transactionId,transactionDate,merchant,amount,receiptNumber\n"
                + "T-001,2026-08-18,A,100,R1\nT-001,2026-08-19,B,200,R2\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("중복 거래 ID");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "settlements.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
