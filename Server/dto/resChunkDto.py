from pydantic import BaseModel
from .resPeerDto import ResPeerDto
from typing import List
from typing import Optional

class ResChunkDto(BaseModel):
    chunk_id : int
    chunk_index : int
    chunk_hash : str
    chunk_size : int
    chunk_peers : List[ResPeerDto] 
