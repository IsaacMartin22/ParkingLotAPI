package parkinglot.common.response;

import lombok.Data;

import java.time.Instant;
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

    private String createdAt;
    private String scheduledAt;

    private String startedAt;
    private String finishedAt;
}