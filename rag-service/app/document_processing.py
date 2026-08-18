from io import BytesIO


def extract_text(filename: str, content: bytes) -> str:
    extension = filename.rsplit(".", 1)[-1].lower()
    if extension in {"md", "txt"}:
        return content.decode("utf-8-sig").strip()
    if extension == "pdf":
        from pypdf import PdfReader

        # 페이지 번호를 인용 정보로 남길 수 있도록 페이지 구분자를 포함함
        pages = [page.extract_text() or "" for page in PdfReader(BytesIO(content)).pages]
        return "\n\n".join(f"[page:{number}]\n{text}" for number, text in enumerate(pages, 1)).strip()
    raise ValueError("PDF, MD, TXT 파일만 등록할 수 있음")


def split_text(text: str, chunk_size: int = 2400, overlap: int = 300) -> list[str]:
    normalized = "\n".join(line.strip() for line in text.splitlines() if line.strip())
    if not normalized:
        raise ValueError("문서에서 검색할 텍스트를 찾지 못함")
    chunks: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(start + chunk_size, len(normalized))
        chunks.append(normalized[start:end])
        if end == len(normalized):
            break
        start = end - overlap
    return chunks
