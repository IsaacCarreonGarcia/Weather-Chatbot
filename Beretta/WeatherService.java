package Beretta;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.*;

// This class gets the weather from the AccuWeather API
public class WeatherService {
    // The key for the AccuWeather API
    private static final String API_KEY = System.getenv ("ACCUWEATHER_API_KEY");
    // First link is to search a city and get its code, second one is for the 5 day forecast
    private static final String LOCATION_URL = "https://dataservice.accuweather.com/locations/v1/cities/search?q=";
    private static final String FORECAST_URL = "https://dataservice.accuweather.com/forecasts/v1/daily/5day/";

    // Client that sends the requests
    private final OkHttpClient client = new OkHttpClient();

    // 5-Day Forecast
    // Returns the forecast of one city as a text
    public String getFiveDayForecast(String city) {
        try {
            // First we need the code (key) of the city
            String locationKey = getLocationKey(city);
            if (locationKey == null) {
                return "Sorry, I couldn't find the city \"" + city + "\".";
            }

            // Make the link with the city code, the key and metric to get Celsius
            String url = FORECAST_URL + locationKey + "?apikey=" + API_KEY + "&metric=true";
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                // Check that the request worked and that we got data
                if (!response.isSuccessful()) return "Failed to retrieve forecast.";

                String responseData = response.body().string();
                if (responseData == null || responseData.isEmpty()) return "Empty forecast data.";

                // Get the list of days from the JSON
                JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                JsonArray dailyForecasts = json.getAsJsonArray("DailyForecasts");

                // StringBuilder to build the final text step by step
                StringBuilder result = new StringBuilder();
                result.append("5-Day Outlook for ").append(city).append(":\n");
                result.append("------------------------------------\n");

                // Variables to calculate the average temperature and count the conditions
                double tempSum = 0.0;
                Map<String, Integer> conditionCount = new HashMap<>();

                // Go through each day of the forecast
                for (int i = 0; i < dailyForecasts.size(); i++) {
                    JsonObject dayForecast = dailyForecasts.get(i).getAsJsonObject();

                    // Take only the date part (first 10 characters) and get the day name
                    String date = dayForecast.get("Date").getAsString().substring(0, 10);
                    String dayName = getDayOfWeek(date);

                    // Get the min and max temperature and the weather text of the day
                    double minTemp = dayForecast.getAsJsonObject("Temperature").getAsJsonObject("Minimum").get("Value").getAsDouble();
                    double maxTemp = dayForecast.getAsJsonObject("Temperature").getAsJsonObject("Maximum").get("Value").getAsDouble();
                    String dayText = dayForecast.getAsJsonObject("Day").get("IconPhrase").getAsString();

                    // Add the average of the day to the total
                    tempSum += (minTemp + maxTemp) / 2.0;

                    // Count how many times each type of weather appears
                    String mainCondition = simplifyCondition(dayText);
                    conditionCount.put(mainCondition, conditionCount.getOrDefault(mainCondition, 0) + 1);

                    // Add one line for this day to the result
                    result.append(String.format("%-3s | %-15s | High: %.0f°C Low: %.0f°C\n",
                            dayName, dayText, maxTemp, minTemp));
                }

                result.append("------------------------------------\n");

                // Average temperature of the 5 days and the weather that appears the most
                double avgTemp = tempSum / dailyForecasts.size();
                String dominantCondition = Collections.max(conditionCount.entrySet(), Map.Entry.comparingByValue()).getKey();

                // Add the clothes and activity suggestions at the end
                result.append("\nRecommendations based on the forecast:\n");
                result.append("- Clothes: ").append(getClothingSuggestion(avgTemp)).append("\n");
                result.append("- Activities: ").append(getActivitySuggestion(dominantCondition)).append("\n");

                return result.toString();
            }
        } catch (IOException e) {
            // Network problem
            return "Error: " + e.getMessage();
        }
    }

    //  Compare Multiple Cities
    // Compares the cities and says which one has more sunny days
    public String compareCities(List<String> cities) {
        // Maps to save the data of each city (the city name is the key)
        Map<String, Integer> sunnyDaysMap = new HashMap<>();
        Map<String, Double> avgTempMap = new HashMap<>();
        // Map for the cities that had a problem
        Map<String, String> errorCities = new HashMap<>();

        for (String city : cities) {
            try {
                // Get the city code, if not found save the error and go to the next city
                String locationKey = getLocationKey(city);
                if (locationKey == null) {
                    errorCities.put(city, "City not found.");
                    continue;
                }

                String url = FORECAST_URL + locationKey + "?apikey=" + API_KEY + "&metric=true";
                Request request = new Request.Builder().url(url).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        errorCities.put(city, "Failed to get forecast.");
                        continue;
                    }

                    String responseData = response.body().string();
                    if (responseData == null || responseData.isEmpty()) {
                        errorCities.put(city, "Empty forecast data.");
                        continue;
                    }

                    JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                    JsonArray dailyForecasts = json.getAsJsonArray("DailyForecasts");

                    // Counters for this city
                    int sunnyDays = 0;
                    double tempSum = 0.0;

                    for (int i = 0; i < dailyForecasts.size(); i++) {
                        JsonObject dayForecast = dailyForecasts.get(i).getAsJsonObject();
                        String dayText = dayForecast.getAsJsonObject("Day").get("IconPhrase").getAsString().toLowerCase();

                        // A day counts as sunny if the text has "sun" or "clear"
                        if (dayText.contains("sun") || dayText.contains("clear")) {
                            sunnyDays++;
                        }

                        // Add the average temperature of the day
                        double minTemp = dayForecast.getAsJsonObject("Temperature").getAsJsonObject("Minimum").get("Value").getAsDouble();
                        double maxTemp = dayForecast.getAsJsonObject("Temperature").getAsJsonObject("Maximum").get("Value").getAsDouble();
                        tempSum += (minTemp + maxTemp) / 2.0;
                    }

                    // Save the results of this city
                    sunnyDaysMap.put(city, sunnyDays);
                    avgTempMap.put(city, tempSum / dailyForecasts.size());
                }
            } catch (IOException e) {
                errorCities.put(city, "Error: " + e.getMessage());
            }
        }

        // If no city worked, there is nothing to compare
        if (sunnyDaysMap.isEmpty()) {
            return "Couldn't retrieve weather for any of the cities.";
        }

        // Find the city with the most sunny days
        String bestCity = Collections.max(sunnyDaysMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        int sunnyDays = sunnyDaysMap.get(bestCity);

        // Build the text with the results of every city
        StringBuilder result = new StringBuilder();
        result.append("Here is the sunny day comparison:\n");
        for (String city : sunnyDaysMap.keySet()) {
            int sunny = sunnyDaysMap.get(city);
            double avgTemp = avgTempMap.get(city);
            result.append(" - ").append(city).append(": ").append(sunny)
                    .append(" sunny days, Avg Temp: ").append(String.format("%.1f", avgTemp)).append("°C\n");
        }
        // Also show the cities that had errors
        for (Map.Entry<String, String> entry : errorCities.entrySet()) {
            result.append(" - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Final recommendation
        result.append("\nRecommendation: I suggest visiting ").append(bestCity)
                .append(", with ").append(sunnyDays).append(" sunny days ahead!\n");

        return result.toString();
    }

    // Get Location Key
    // Searches the city and returns its code, or null if it isn't found
    private String getLocationKey(String city) throws IOException {
        city = city.trim();

        // Special handling for famous cities
        // I added the country because there are more cities with the same name
        if (city.equalsIgnoreCase("London")) {
            city = "London, United Kingdom";
        } else if (city.equalsIgnoreCase("Paris")) {
            city = "Paris, France";
        } else if (city.equalsIgnoreCase("Madrid")) {
            city = "Madrid, Spain";
        }

        String encodedCity = city.replace(" ", "%20"); // URL encode
        String url = LOCATION_URL + encodedCity + "&apikey=" + API_KEY;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            // Return null if something goes wrong
            if (!response.isSuccessful()) return null;

            String responseData = response.body().string();
            if (responseData == null || responseData.isEmpty()) return null;

            // The API returns a list, we use the first result
            JsonArray jsonArray = JsonParser.parseString(responseData).getAsJsonArray();
            if (jsonArray.size() == 0) return null;

            JsonObject json = jsonArray.get(0).getAsJsonObject();
            return json.get("Key").getAsString();
        }
    }

    //  Get day of the week
    // Changes a date like 2025-05-10 into the short day name
    private String getDayOfWeek(String dateStr) {
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        return date.getDayOfWeek().toString().substring(0, 3); // MON, TUE, etc.
    }

    // Simplify weather condition
    // The API has a lot of different texts, so I group them in 4 types
    private String simplifyCondition(String condition) {
        condition = condition.toLowerCase();
        if (condition.contains("sun") || condition.contains("clear")) return "sun";
        if (condition.contains("cloud")) return "cloud";
        if (condition.contains("rain") || condition.contains("storm") || condition.contains("shower")) return "rain";
        if (condition.contains("snow")) return "snow";
        return "unknown";
    }

    //  Suggest clothes
    // Depends on the average temperature
    private String getClothingSuggestion(double temp) {
        if (temp < 10) return "Wear a heavy jacket, gloves, and scarf.";
        else if (temp < 20) return "You might want a light jacket or sweater.";
        else if (temp < 28) return "A t-shirt and jeans should be fine.";
        else return "It's hot! Wear shorts, sunglasses, and stay hydrated.";
    }

    //  Suggest activities
    // Depends on the weather that appears the most
    private String getActivitySuggestion(String condition) {
        switch (condition) {
            case "sun": return "Perfect for a walk, picnic, or outdoor sports!";
            case "cloud": return "Good day for a museum visit, shopping, or cozy cafe time.";
            case "rain": return "Better to stay indoors — maybe watch a movie or read.";
            case "snow": return "Great time for skiing, snowboarding, or building a snowman!";
            default: return "Use your best judgment. Weather may vary.";
        }
    }
}
