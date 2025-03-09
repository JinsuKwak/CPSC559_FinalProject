package p2pclient.dto;

public class ReqPeerDto {
    public String peer_ip;
    public Integer peer_port;
    private Long chunk_id; // Optional

    public ReqPeerDto() {}
    public ReqPeerDto(String peer_ip, int peer_port) {
        this.peer_ip = peer_ip;
        this.peer_port = peer_port;
    }

    public ReqPeerDto(String peer_ip, int peer_port, Long chunk_id) {
        this.peer_ip = peer_ip;
        this.peer_port = peer_port;
    }

    public Long getChunk_id() {
        return chunk_id;
    }
}