import os

import chromadb
from chromadb.utils.embedding_functions import OpenAIEmbeddingFunction


class PolicyVectorStore:
    def __init__(self) -> None:
        api_key = os.environ.get("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError("OPENAI_API_KEY가 필요함")
        client = chromadb.PersistentClient(path=os.environ.get("CHROMA_PATH", "./data/chroma"))
        embedding = OpenAIEmbeddingFunction(
            api_key=api_key,
            model_name=os.environ.get("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"),
        )
        self.collection = client.get_or_create_collection(
            name="settlement_policies",
            embedding_function=embedding,
            metadata={"hnsw:space": "cosine"},
        )

    def index(self, document_id: int, title: str, version: int, chunks: list[str]) -> None:
        ids = [f"policy-{document_id}-v{version}-{number}" for number in range(len(chunks))]
        # 같은 문서 버전을 다시 처리해도 중복 청크가 생기지 않도록 upsert함
        self.collection.upsert(
            ids=ids,
            documents=chunks,
            metadatas=[{
                "document_id": document_id,
                "title": title,
                "version": version,
                "chunk_number": number,
                "status": "ACTIVE",
            } for number in range(len(chunks))],
        )

    def search(self, query: str, limit: int = 4) -> list[dict]:
        result = self.collection.query(query_texts=[query], n_results=limit)
        documents = result.get("documents", [[]])[0]
        metadata = result.get("metadatas", [[]])[0]
        return [{"text": text, **meta} for text, meta in zip(documents, metadata)]
