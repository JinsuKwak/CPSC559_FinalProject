from pydantic import BaseModel, Field
from .reqPeerDto import ReqPeerDto
from typing import List
from typing import Optional


# None for GET requests
class ReqChunkDto(BaseModel):
    chunk_index : int
    chunk_hash : str
    chunk_size : int
    chunk_peers : List[ReqPeerDto]
