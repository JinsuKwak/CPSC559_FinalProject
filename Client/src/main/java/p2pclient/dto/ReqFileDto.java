package p2pclient.dto;

import java.util.List;

public class ReqFileDto {
    public String file_name;
    public String file_hash;
    public Long file_size;
    public Boolean file_enc;
    public List<ReqChunkDto> file_chunks;

    public ReqFileDto() {
    }

    public ReqFileDto(String file_name, String file_hash, Long file_size, Boolean file_enc) {
        this.file_name = file_name;
        this.file_hash = file_hash;
        this.file_size = file_size;
        this.file_enc = file_enc;
    }

    public void setFile_chunks(List<ReqChunkDto> file_chunks) {
        this.file_chunks = file_chunks;
    }
}