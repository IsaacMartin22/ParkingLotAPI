package parkinglot.common.response;

public record DeploymentResponse(
        Deploy deploy,
        String cursor
) {
    public record Deploy(
            String id,
            Commit commit,
            String status,
            String trigger,
            String startedAt,
            String finishedAt,
            String createdAt,
            String updatedAt
    ) {
    }

    public record Commit(
            String id,
            String message,
            String createdAt
    ) {
    }
}
