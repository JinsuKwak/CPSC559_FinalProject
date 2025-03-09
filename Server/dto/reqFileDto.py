from pydantic import BaseModel, Field
from .reqChunkDto import ReqChunkDto
from typing import List
from typing import Optional

# Upload File Request
class ReqFileDto(BaseModel):
    file_name : str
    file_hash : str
    file_size : int
    file_enc : bool
    file_chunks: List[ReqChunkDto]