from pydantic import BaseModel
from typing import Optional

# None for add / remove peers on proxy
# chunk_id is need for hard add / remove peers on Database
class ReqPeerDto(BaseModel):
    chunk_id : Optional[int] = None
    peer_ip : str
    peer_port : int
