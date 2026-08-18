import unittest

from app.document_processing import extract_text, split_text


class DocumentProcessingTest(unittest.TestCase):
    def test_extracts_utf8_text_and_splits_with_overlap(self) -> None:
        text = extract_text("policy.md", "정산 규정\n고액 거래 승인 필요".encode())
        chunks = split_text(text, chunk_size=10, overlap=2)

        self.assertGreater(len(chunks), 1)
        self.assertIn("정산 규정", chunks[0])

    def test_rejects_empty_document(self) -> None:
        with self.assertRaises(ValueError):
            split_text(" \n ")


if __name__ == "__main__":
    unittest.main()
