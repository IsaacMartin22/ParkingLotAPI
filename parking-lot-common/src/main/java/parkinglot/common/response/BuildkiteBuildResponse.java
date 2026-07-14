package parkinglot.common.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BuildkiteBuildResponse {

    private String id;

    private Integer number;
    private String state;
    private Boolean blocked;

    private String cancelReason;

    private String message;
    private String commit;
    private String branch;

    private String source;

    private BuildkitePipeline pipeline;

    private OffsetDateTime createdAt;
    private OffsetDateTime scheduledAt;

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}