package com.namhyerin.settlement.review.application;

import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.exception.InvalidCsvException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CsvSettlementParser {

    private static final List<String> HEADER = List.of(
            "transactionId", "transactionDate", "merchant", "amount", "receiptNumber");

    public List<SettlementTransaction> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV 파일이 비어 있습니다.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            // UTF-8 BOM을 제거한 뒤 필수 헤더와 순서까지 검증함
            if (firstLine == null || !parseLine(firstLine.replace("\uFEFF", "")).equals(HEADER)) {
                throw new InvalidCsvException("필수 열이 없거나 순서가 올바르지 않습니다: " + String.join(",", HEADER));
            }

            List<SettlementTransaction> transactions = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                List<String> values = parseLine(line);
                if (values.size() != HEADER.size()) {
                    throw invalid(lineNumber, "열 개수가 올바르지 않습니다.");
                }
                String id = values.get(0).trim();
                String merchant = values.get(2).trim();
                if (id.isBlank() || merchant.isBlank()) {
                    throw invalid(lineNumber, "거래 ID와 거래처는 필수입니다.");
                }
                // 같은 업로드 안의 거래 ID 중복을 차단함
                if (!ids.add(id)) {
                    throw invalid(lineNumber, "중복 거래 ID입니다: " + id);
                }
                try {
                    BigDecimal amount = new BigDecimal(values.get(3).trim());
                    if (amount.signum() <= 0) throw invalid(lineNumber, "금액은 0보다 커야 합니다.");
                    transactions.add(new SettlementTransaction(id, LocalDate.parse(values.get(1).trim()), merchant,
                            amount, values.get(4).trim().isEmpty() ? null : values.get(4).trim()));
                } catch (NumberFormatException e) {
                    throw invalid(lineNumber, "금액 형식이 올바르지 않습니다.");
                } catch (DateTimeParseException e) {
                    throw invalid(lineNumber, "날짜 형식은 yyyy-MM-dd여야 합니다.");
                }
            }
            if (transactions.isEmpty()) throw new InvalidCsvException("CSV에 거래 내역이 없습니다.");
            return transactions;
        } catch (IOException e) {
            throw new InvalidCsvException("CSV 파일을 읽을 수 없습니다.");
        }
    }

    private InvalidCsvException invalid(int line, String message) {
        return new InvalidCsvException(line + "행: " + message);
    }

    private List<String> parseLine(String line) {
        // 큰따옴표 안의 쉼표와 이스케이프 큰따옴표를 함께 처리함
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else value.append(c);
        }
        if (quoted) throw new InvalidCsvException("닫히지 않은 큰따옴표가 있습니다.");
        values.add(value.toString());
        return values;
    }
}
