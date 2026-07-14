package parkinglot.common.response;

import lombok.Data;

@Data
public class BuildkiteBuildResponse {

    private String id;

    private Integer number;
    private String state;
    private Boolean blocked;

    private String cancel_reason;

    private String message;
    private String commit;
    private String branch;

    private String source;

    private BuildkitePipeline pipeline;

    private String created_at;
    private String scheduled_at;

    private String started_at;
    private String finished_at;
}