package Beretta;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

// This class is the window of the chatbot (the GUI), it extends JFrame so it is a window
public class HeloiseGUI extends JFrame {

    // The two services I made: one for the chat and one for the weather
    private final ChatBotService chatBotService = new ChatBotService();
    private final WeatherService weatherService = new WeatherService();
    // Area where the messages are shown
    private final JTextPane chatPane;
    // Place where the user writes
    private final JTextField userInputField;
    private final JButton sendButton;
    // The document of the chat pane, I use it to add text with colors
    private final StyledDocument doc;
    // Styles for the messages (one for the user and one for the bot)
    private final Style userStyle;
    private final Style botStyle;

    public HeloiseGUI() {
        // Window settings: title, close the program when closing the window, size and layout
        setTitle("Chat with Heloise ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLayout(new BorderLayout());

        // Chat area, the user can't edit it, only read
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.WHITE);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        // Style for the user messages (blue)
        doc = chatPane.getStyledDocument();
        userStyle = chatPane.addStyle("UserStyle", null);
        StyleConstants.setForeground(userStyle, new Color(30, 144, 255)); // DodgerBlue
        StyleConstants.setFontSize(userStyle, 16);
        StyleConstants.setFontFamily(userStyle, "Segoe UI");

        // Style for the bot messages (yellow and bold)
        botStyle = chatPane.addStyle("BotStyle", null);
        StyleConstants.setForeground(botStyle, new Color(255, 191, 0)); // Snapchat Yellow
        StyleConstants.setFontSize(botStyle, 16);
        StyleConstants.setFontFamily(botStyle, "Segoe UI");
        StyleConstants.setBold(botStyle, true);

        // Scroll bar so we can go up when the chat is long
        JScrollPane scrollPane = new JScrollPane(chatPane);

        userInputField = new JTextField();
        sendButton = new JButton("Send");

        // Panel at the bottom with the text field in the middle and the button on the right
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(userInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Put the chat in the center and the input panel at the bottom
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Send the message when clicking the button or pressing Enter
        sendButton.addActionListener(new SendButtonListener());
        userInputField.addActionListener(new SendButtonListener());

        // First message when the program starts
        appendBotMessage("Hi! I'm Heloise! How can I help you today?");

        setVisible(true);
    }

    // Inner class that runs when the user sends a message
    private class SendButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            // Get the text and remove the extra spaces
            String userText = userInputField.getText().trim();
            // Only continue if the user wrote something
            if (!userText.isEmpty()) {
                appendUserMessage(userText);
                userInputField.setText("");

                String botReply;

                // Check if the user wrote a special command, if not send it to the chatbot
                if (userText.equalsIgnoreCase("exit")) {
                    System.exit(0);
                    return;
                } else if (userText.equalsIgnoreCase("clear memory")) {
                    chatBotService.clearMemory();
                    botReply = " Memory cleared!";
                } else if (userText.equalsIgnoreCase("save chat")) {
                    chatBotService.saveConversation("chat_history.txt");
                    botReply = " Chat history saved.";
                } else if (userText.toLowerCase().contains("compare")) {
                    // Compare the weather of different cities
                    botReply = handleCityComparison();
                } else if (userText.toLowerCase().contains("weather") || userText.toLowerCase().contains("forecast")) {
                    // Weather of only one city
                    botReply = handleSingleCityWeather();
                } else {
                    botReply = chatBotService.getResponse(userText);
                }

                // Show the answer in the chat
                appendBotMessage(botReply);
            }
        }

        // Asks the user for the cities and compares the weather
        private String handleCityComparison() {
            int numberOfCities = 0;
            try {
                // Pop up window to ask how many cities
                String input = JOptionPane.showInputDialog("How many cities do you want to compare? (max 6):");
                // If the user presses cancel, the input is null
                if (input == null) return "❗ Comparison cancelled.";
                numberOfCities = Integer.parseInt(input.trim());
            } catch (Exception ex) {
                // This happens if the user doesn't write a number
                return "❗ Invalid number.";
            }

            // The number has to be between 2 and 6
            if (numberOfCities < 2 || numberOfCities > 6) {
                return "❗ You must compare between 2 and 6 cities.";
            }

            // Ask for each city and save them in a list
            List<String> cities = new ArrayList<>();
            for (int i = 1; i <= numberOfCities; i++) {
                String city = JOptionPane.showInputDialog("Enter city " + i + ":");
                // Skip the city if it is empty or cancelled
                if (city != null && !city.trim().isEmpty()) {
                    cities.add(capitalizeEachWord(city.trim()));
                }
            }

            return weatherService.compareCities(cities);
        }

        // Asks for one city and returns the 5 day forecast
        private String handleSingleCityWeather() {
            String city = JOptionPane.showInputDialog("For which city?");
            if (city == null || city.trim().isEmpty()) {
                return "❗ No city entered.";
            }
            return weatherService.getFiveDayForecast(capitalizeEachWord(city.trim()));
        }
    }

    // Adds the user message to the chat with the blue style
    private void appendUserMessage(String message) {
        try {
            doc.insertString(doc.getLength(), "You: " + message + "\n", userStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Adds the bot message to the chat with the yellow style
    private void appendBotMessage(String message) {
        try {
            doc.insertString(doc.getLength(), "Heloise: " + message + "\n\n", botStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Makes the first letter of each word a capital letter (for example "new york" becomes "New York")
    private static String capitalizeEachWord(String input) {
        // Split the text into words using the spaces
        String[] words = input.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            // Ignore empty words (when there are double spaces)
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        // Remove the last space at the end
        return capitalized.toString().trim();
    }

    // Starts the program
    public static void main(String[] args) {
        // invokeLater runs the GUI in the right thread for Swing
        SwingUtilities.invokeLater(HeloiseGUI::new);
    }
}