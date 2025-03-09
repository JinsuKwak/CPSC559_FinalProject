package p2pclient.dto;

import java.util.List;

public class ReqChunkDto {
    public Long chunk_index;
    public String chunk_hash;
    public Integer chunk_size;
    public List<ReqPeerDto> chunk_peers; // peer info

    public ReqChunkDto() {}

    public ReqChunkDto(Long chunk_index, String chunk_hash, Integer chunk_size) {
        this.chunk_index = chunk_index;
        this.chunk_hash = chunk_hash;
        this.chunk_size = chunk_size;
    }

    public void setChunk_peers(List<ReqPeerDto> chunk_peers) {
        this.chunk_peers = chunk_peers;
    }
}