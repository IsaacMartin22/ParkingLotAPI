package apiservice.service;

public interface ChatService {
    String ask(String question);

    String ask(String question, String model);

    ChatAnswer askWithCitation(String question);

    ChatAnswer askWithCitation(String question, String model);
}
