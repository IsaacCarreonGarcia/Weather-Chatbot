package Beretta;

import okhttp3.*;
import com.google.gson.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
// This class connects the chatbot to the OpenAI API and keeps the conversation going
public class ChatBotService {
 
    // The link where we send our messages
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    // I get the key from an environment variable so it is not written in the code
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    // This is the client that sends the requests
    private final OkHttpClient client;
    // List that saves all the messages so the bot remembers the conversation
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
 
    public ChatBotService() {
        // Stop the program if the API key is missing
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not set. Please set the OPENAI_API_KEY environment variable.");
        }
        // Make the client and give it a 30 second limit so it doesn't wait forever
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .build();
 
        // System prompt
        // This first message tells the bot who it is
        Map<String, String> systemPrompt = new HashMap<>();
        systemPrompt.put("role", "system");
        systemPrompt.put("content", "You are Beretta 2.0, a helpful, friendly, and smart assistant.");
        conversationHistory.add(systemPrompt);
    }
 
    // Takes what the user wrote and returns the answer from the bot
    public String getResponse(String userInput) {
        // Save the user message in the history first
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userInput);
        conversationHistory.add(userMessage);
 
        try {
            // Build the JSON that we send to the API
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "gpt-3.5-turbo");
 
            // Put every message from the history into the JSON array
            JsonArray messagesArray = new JsonArray();
            for (Map<String, String> message : conversationHistory) {
                JsonObject messageJson = new JsonObject();
                messageJson.addProperty("role", message.get("role"));
                messageJson.addProperty("content", message.get("content"));
                messagesArray.add(messageJson);
            }
            requestBody.add("messages", messagesArray);
 
            // Make the request with the key in the header
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            new Gson().toJson(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
 
            // Send the request (try-with-resources closes the response by itself)
            try (Response response = client.newCall(request).execute()) {
                // If something went wrong, return an error message instead
                if (!response.isSuccessful()) {
                    return "Sorry, I had trouble reaching OpenAI: " + response.message();
                }
 
                // Read the answer and take the text of the reply out of the JSON
                String responseData = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseData).getAsJsonObject();
                String reply = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString()
                        .trim();
 
                // Add assistant message to memory
                // So the bot remembers what it said before
                Map<String, String> assistantMessage = new HashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", reply);
                conversationHistory.add(assistantMessage);
 
                return reply;
            }
 
        } catch (IOException e) {
            // This happens if there is a network problem
            return "Error: " + e.getMessage();
        }
    }
 
    // Deletes the whole conversation and starts again
    public void clearMemory() {
        conversationHistory.clear();
        // Add the system prompt again, if not the bot forgets who it is
        Map<String, String> systemPrompt = new HashMap<>();
        systemPrompt.put("role", "system");
        systemPrompt.put("content", "You are Beretta 2.0, a helpful, friendly, and smart assistant.");
        conversationHistory.add(systemPrompt);
    }
 
    // Saves the conversation in a text file
    public void saveConversation(String filename) {
        try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
            // Write each message in a new line like [role] message
            for (Map<String, String> message : conversationHistory) {
                writer.write("[" + message.get("role") + "] " + message.get("content") + "\n");
            }
        } catch (IOException e) {
            // Print the error if the file could not be saved
            System.err.println("Failed to save conversation: " + e.getMessage());
        }
    }
}
 