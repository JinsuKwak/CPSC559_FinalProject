from pydantic import BaseModel
from typing import Optional

class ResPeerDto(BaseModel):
    peer_ip : str
    peer_port : int