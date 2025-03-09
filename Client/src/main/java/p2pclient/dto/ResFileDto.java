package p2pclient.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResFileDto {
    public Integer file_id;
    public String file_name;
    public String file_hash;
    public Long file_size;
    public boolean file_enc;
    public List<ResChunkDto> file_chunks; // for getAllFiles()

    public ResFileDto() {}

    // for `getAllFiles()`
    public ResFileDto(int file_id, String file_name, String file_hash, long file_size, boolean file_enc) {
        this.file_id = file_id;
        this.file_name = file_name;
        this.file_hash = file_hash;
        this.file_size = file_size;
        this.file_enc = file_enc;
        this.file_chunks = null;
    }

    // for `downloadFile()`
    public ResFileDto(int file_id, String file_name, String file_hash, long file_size, boolean file_enc, List<ResChunkDto> file_chunks) {
        this.file_id = file_id;
        this.file_name = file_name;
        this.file_hash = file_hash;
        this.file_size = file_size;
        this.file_enc = file_enc;
        this.file_chunks = file_chunks;
    }
}