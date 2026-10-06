package Beretta;

import java.util.*;

// This is the console version of the chatbot (no window, only the terminal)
public class Main {
    public static void main(String[] args) {
        // Scanner reads what the user types
        Scanner scanner = new Scanner(System.in);
        // The two services: weather and chat
        WeatherService weatherService = new WeatherService();
        ChatBotService chatBotService = new ChatBotService();

        System.out.println("Hello, I am Beretta 2.0! What can I help you with? (type 'exit' to quit)");

        // Loop that keeps running until the user writes exit
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            // If the user only pressed Enter, ask again
            if (userInput.isEmpty()) {
                System.out.println("Please enter something!");
                continue;
            }

            // Exit command, break stops the loop
            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye! ");
                break;
            }

            // Command to delete the conversation memory
            if (userInput.equalsIgnoreCase("clear memory")) {
                chatBotService.clearMemory();
                System.out.println("Memory cleared! Starting fresh.");
                continue;
            }

            // Command to save the conversation in a text file
            if (userInput.equalsIgnoreCase("save chat")) {
                chatBotService.saveConversation("chat_history.txt");
                System.out.println(" Chat history saved to chat_history.txt");
                continue;
            }

            // Try-catch so the program doesn't crash if something fails
            try {
                // Case 1: the user wants to compare cities
                if (isComparisonRequest(userInput)) {
                    System.out.print("\nHow many cities would you like to compare? (max 6): ");
                    int numberOfCities = 0;
                    try {
                        // Change the text to a number
                        numberOfCities = Integer.parseInt(scanner.nextLine().trim());
                    } catch (NumberFormatException e) {
                        // The user didn't write a number
                        System.out.println("❗ Please enter a valid number.");
                        continue;
                    }

                    // Only 2 to 6 cities are allowed
                    if (numberOfCities < 2 || numberOfCities > 6) {
                        System.out.println("❗ You must compare between 2 and 6 cities.");
                        continue;
                    }

                    // Ask for each city and save it in the list
                    List<String> cities = new ArrayList<>();
                    for (int i = 1; i <= numberOfCities; i++) {
                        System.out.print("Enter city " + i + ": ");
                        String city = scanner.nextLine().trim();
                        // Don't add empty cities
                        if (!city.isEmpty()) {
                            cities.add(capitalizeEachWord(city));
                        }
                    }

                    // Show the cities and then the comparison
                    System.out.println("\nComparing weather for: " + String.join(", ", cities) + "...");
                    System.out.println(weatherService.compareCities(cities));

                // Case 2: the user asks for the weather
                } else if (isWeatherRequest(userInput)) {
                    // Try to find the city inside the sentence
                    List<String> cities = extractCitiesFromInput(userInput);

                    // If there is no city in the sentence, ask for it
                    if (cities.isEmpty()) {
                        System.out.print("Which city? ");
                        String manualInput = scanner.nextLine();
                        cities = extractCitiesFromInput(manualInput);
                    }

                    // For the weather we only accept one city
                    if (cities.size() == 1) {
                        System.out.println("\nFetching weather for " + cities.get(0) + "...");
                        System.out.println(weatherService.getFiveDayForecast(cities.get(0)));
                    } else {
                        System.out.println("Please only ask for one city when checking weather. To compare multiple cities, say 'compare cities'.");
                    }

                // Case 3: anything else goes to the chatbot
                } else {
                    System.out.println("Bot: " + chatBotService.getResponse(userInput));
                }
            } catch (Exception e) {
                // Show the error message
                System.err.println("An error occurred: " + e.getMessage());
            }
        }

        // Close the scanner when we finish
        scanner.close();
    }

    // Checks if the user is asking about the weather (looks for some words)
    private static boolean isWeatherRequest(String input) {
        input = input.toLowerCase();
        return input.contains("weather") || input.contains("forecast") || input.contains("temperature") || input.contains("climate");
    }

    // Checks if the user wants to compare cities
    private static boolean isComparisonRequest(String input) {
        input = input.toLowerCase();
        return input.contains("compare") || input.contains("versus") || input.contains("vs");
    }

    // Tries to get the city names from the sentence, for example "weather in dublin"
    public static List<String> extractCitiesFromInput(String input) {
        List<String> cities = new ArrayList<>();
        input = input.toLowerCase();

        // Small words that usually come before the city name
        String[] keywords = {"in", "for", "of", "at"};

        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                // Take everything that is after the keyword
                int index = input.indexOf(keyword) + keyword.length();
                String afterKeyword = input.substring(index).trim();

                // Remove words that are not part of the city name
                afterKeyword = afterKeyword.replaceAll("(the weather|climate|forecast|temperature)", "").trim();

                // Split in case there is more than one city (comma, and, vs, versus)
                String[] parts = afterKeyword.split("(,| and | vs | versus )");

                for (String part : parts) {
                    String city = capitalizeEachWord(part.trim());
                    if (!city.isEmpty()) {
                        cities.add(city);
                    }
                }
                // Stop after the first keyword that works
                break;
            }
        }

        return cities;
    }

    // Makes the first letter of each word a capital letter (same method as in the GUI class)
    private static String capitalizeEachWord(String input) {
        String[] words = input.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            // Skip empty words
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        // Remove the last space
        return capitalized.toString().trim();
    }
}