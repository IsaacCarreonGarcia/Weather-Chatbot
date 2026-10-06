# Beretta / Heloise Chatbot

A chatbot I made in Java as a college project. It can talk about anything using the OpenAI API and it can also give weather forecasts and compare the weather of different cities using the AccuWeather API.

The project has two ways to run it:
- **Console version** (`Main.java`): you chat in the terminal. The bot is called Beretta 2.0.
- **Window version** (`HeloiseGUI.java`): a chat window made with Java Swing. The bot is called Heloise.

## Features

- Chat with an AI that remembers the conversation
- 5-day weather forecast for any city, with clothes and activity suggestions
- Compare the weather of 2 to 6 cities and see which one has more sunny days
- Save the conversation to a text file
- Clear the bot's memory and start again

## Commands

| What you write | What happens |
| --- | --- |
| `weather in Dublin` (or anything with "weather" / "forecast") | Shows the 5-day forecast for that city |
| `compare cities` (or anything with "compare") | Asks for the cities and compares them |
| `save chat` | Saves the conversation in `chat_history.txt` |
| `clear memory` | Deletes the conversation memory |
| `exit` | Closes the program |

Anything else is sent to the chatbot.

## Project structure

```
Beretta/
├── Main.java            # console version
├── HeloiseGUI.java      # window version (Swing)
├── ChatBotService.java  # connects to the OpenAI API and keeps the history
└── WeatherService.java  # connects to the AccuWeather API
```

## Technologies

- Java
- [OkHttp](https://square.github.io/okhttp/) (version 4) for the HTTP requests
- [Gson](https://github.com/google/gson) for reading and writing JSON
- Java Swing for the window
- OpenAI API (`gpt-3.5-turbo`)
- AccuWeather API

## How to run it

### 1. Requirements

- Java JDK installed
- The OkHttp and Gson libraries added to the project (OkHttp also needs Okio and the Kotlin standard library). The easiest way is to use an IDE like IntelliJ or Eclipse and add them as dependencies.
- An [OpenAI API key](https://platform.openai.com/)
- An [AccuWeather API key](https://developer.accuweather.com/)

### 2. Set the API keys

The keys are **not** written in the code. They are read from environment variables:

```bash
# Linux / macOS
export OPENAI_API_KEY="your_openai_key"
export ACCUWEATHER_API_KEY="your_accuweather_key"
```

```powershell
# Windows (PowerShell)
$env:OPENAI_API_KEY="your_openai_key"
$env:ACCUWEATHER_API_KEY="your_accuweather_key"
```

If the OpenAI key is missing, the program stops with an error message.

### 3. Run

- Console version: run `Main.java`
- Window version: run `HeloiseGUI.java`

## Known limitations

- The city detection in the console version looks for small words like "in" or "for", so it can fail with some sentences.
- The weather part only works with cities that AccuWeather can find.
- The window version asks for the cities using pop-up windows.
- The API usage can have costs or limits depending on your account.

## About

Made as a student project to practise Java, working with APIs, JSON, and building a simple GUI.
