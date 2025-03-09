package p2pclient.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
public class ResPeerDto {
    public Integer chunk_id; // for downloadFile()
    public String peer_ip;
    public Integer peer_port;

    public ResPeerDto() {}

    public ResPeerDto(Integer chunk_id, String peer_ip) {
        this.chunk_id = chunk_id;
        this.peer_ip = peer_ip;
        this.peer_port = null;
    }

    public ResPeerDto(Integer chunk_id, String peer_ip, int peer_port) {
        this.chunk_id = chunk_id;
        this.peer_ip = peer_ip;
        this.peer_port = peer_port;
    }
}