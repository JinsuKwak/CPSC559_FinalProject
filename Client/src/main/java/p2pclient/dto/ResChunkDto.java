package p2pclient.dto;

import java.util.List;

public class ResChunkDto {
    public Integer chunk_id;
    public Long chunk_index;
    public String chunk_hash;
    public Integer chunk_size;
    public List<ResPeerDto> chunk_peers;

    public ResChunkDto() {}
}