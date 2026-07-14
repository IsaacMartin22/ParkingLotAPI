package parkinglot.common.request;

public record TrelloFeatureRequest(
        String cardTitle,
        String cardMessage
) {}
