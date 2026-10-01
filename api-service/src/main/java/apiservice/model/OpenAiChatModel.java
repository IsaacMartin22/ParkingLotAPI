package apiservice.model;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum OpenAiChatModel {
    GPT_5_6("gpt-5.6"),
    GPT_5_6_CYBER("gpt-5.6-cyber"),
    GPT_6("gpt-6"),
    GPT_6_1("gpt-6.1"),
    GPT_6_1_SOL("gpt-6.1-sol"),
    GPT_6_ASTRA("gpt-6-astra"),
    GPT_6_LUNA("gpt-6-luna"),
    GPT_6_SOL("gpt-6-sol");

    private static final Map<String, OpenAiChatModel> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toMap(model -> model.value.toLowerCase(), model -> model));

    private final String value;

    OpenAiChatModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OpenAiChatModel fromValue(String value) {
        if (value == null) {
            return GPT_6_1_SOL;
        }

        OpenAiChatModel model = BY_VALUE.get(value.trim().toLowerCase());
        if (model == null) {
            throw new IllegalArgumentException(
                    "Unsupported chat model '" + value + "'. Allowed models: " + Arrays.stream(values())
                            .map(OpenAiChatModel::getValue)
                            .sorted()
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("")
            );
        }
        return model;
    }
}
