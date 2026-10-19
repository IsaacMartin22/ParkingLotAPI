package parkinglot.common.response;

import lombok.Data;

@Data
public class GubbResponse {
    private String url;
    private String jobId;
    private String status;
    private String statusUrl;
    private String error;

    public GubbResponse(String url) {
        this.url = url;
    }

    public GubbResponse(String url, String jobId, String status, String statusUrl, String error) {
        this.url = url;
        this.jobId = jobId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.error = error;
    }
}