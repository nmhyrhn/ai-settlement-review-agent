import base64
import hmac
import os
from pathlib import Path
from typing import Optional, Union

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
from dotenv import load_dotenv
from openai import OpenAI

from .chroma_store import PolicyVectorStore
from .document_processing import extract_text, split_text

# Spring Boot와 Python이 같은 로컬 환경 설정을 사용하도록 저장소의 .env를 읽음
load_dotenv(Path(__file__).resolve().parents[2] / ".env")

app = FastAPI(title="Settlement Policy RAG Service")


class DocumentRequest(BaseModel):
    documentId: int
    title: str = Field(min_length=1, max_length=200)
    version: int = Field(gt=0)
    filename: str
    contentType: Optional[str] = None
    contentBase64: str


class ExplanationRequest(BaseModel):
    transactionId: str
    amount: str
    merchant: str
    violationCodes: list[str]
    reasons: list[str]


def verify_internal_key(x_internal_api_key: str = Header()) -> None:
    expected = os.environ.get("RAG_SERVICE_API_KEY", "local-rag-secret")
    if not hmac.compare_digest(x_internal_api_key, expected):
        raise HTTPException(status_code=401, detail="내부 API 키가 올바르지 않음")


@app.get("/internal/health", dependencies=[Depends(verify_internal_key)])
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/internal/documents", dependencies=[Depends(verify_internal_key)])
def register_document(request: DocumentRequest) -> dict[str, Union[int, str]]:
    try:
        content = base64.b64decode(request.contentBase64, validate=True)
        chunks = split_text(extract_text(request.filename, content))
        PolicyVectorStore().index(request.documentId, request.title, request.version, chunks)
        return {"status": "INDEXED", "chunkCount": len(chunks)}
    except (ValueError, UnicodeDecodeError) as exception:
        raise HTTPException(status_code=400, detail=str(exception)) from exception


@app.post("/internal/explanations", dependencies=[Depends(verify_internal_key)])
def explain(request: ExplanationRequest) -> dict:
    query = " ".join(request.violationCodes + request.reasons)
    sources = PolicyVectorStore().search(query)
    context = "\n\n".join(source["text"] for source in sources)
    prompt = f"""정산 검수 담당자에게 아래 위반을 한국어로 간결하게 설명함.
거래처: {request.merchant}, 금액: {request.amount}, 위반: {query}
반드시 제공된 정책 근거만 사용함. 관련 근거가 없으면 없다고 명시함.
정책 근거:\n{context}"""
    summary = OpenAI().responses.create(
        model=os.environ.get("OPENAI_MODEL", "gpt-4.1-mini"), input=prompt
    ).output_text
    # 실제 검색에 사용한 청크 정보를 설명과 함께 반환함
    citations = [{key: source[key] for key in ("document_id", "title", "version", "chunk_number")}
                 for source in sources]
    return {"summary": summary, "citations": citations, "generatedBy": "OPENAI_RAG"}
