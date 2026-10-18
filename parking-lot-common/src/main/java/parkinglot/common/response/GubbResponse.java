package parkinglot.common.response;

import lombok.Data;

@Data
public class GubbResponse {
    private String url;

    public GubbResponse(String url) {
        this.url = url;
    }
}