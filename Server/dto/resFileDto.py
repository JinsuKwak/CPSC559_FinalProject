from pydantic import BaseModel
from .resChunkDto import ResChunkDto
from typing import List
from typing import Optional

class ResFileDto(BaseModel):
    file_id : int
    file_name : str
    file_hash : str
    file_size : int 
    file_enc : bool 
    file_chunks : List[ResChunkDto] 