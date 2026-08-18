import base64
import hmac
import os
from pathlib import Path
from typing import Optional, Union

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
from dotenv import load_dotenv

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
