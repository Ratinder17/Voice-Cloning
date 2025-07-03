package com.example.Voice.Cloning.service;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class Translation {

    public String translate(String text, String targetLang) throws IOException {
        URL url = new URL("https://api-free.deepl.com/v2/translate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String api = "3f4387f5-3bea-49b2-ad3c-d0d5a8465ddb:fx";
        String data = "auth_key=" + URLEncoder.encode(api, StandardCharsets.UTF_8) +
                "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&target_lang=" + URLEncoder.encode(targetLang, StandardCharsets.UTF_8);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes());
        }

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Translation API request failed with code: " + status);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String response = in.lines().collect(Collectors.joining());
            JSONObject json = new JSONObject(response);
            return json.getJSONArray("translations").getJSONObject(0).getString("text");
        }
    }

}


/*
THOUGHT PROCESS -

Need to leverage an API to translate the text.
   API selection
      Options:
         1. DeepL
         2. Google Translate - Better for short phrases
         3. Microsoft Azure Translator - Built for technical/enterprise
         4. Amazon Translate - Only makes sense if already in AWS ecosystem
         5. ChatGPT API - Slower  because of LLM overhead

         DeepL's Translation API makes most sense because of its accuracy, wide support and because
         it works well even with long paragraphs, which maybe very helpful when scaling.

       Send a Post request
       Sample request
        Copy
        curl -X POST https://api.deepl.com/v2/translate \
        --header "Content-Type: application/json" \
        --header "Authorization: DeepL-Auth-Key $API_KEY" \
        --data '{
         "text": ["Hello world!"],
         "target_lang": "DE"
        }'

        Send a Post request based on the documentation above and store the output as a string

        Then read the HTTP response body from server
         Store it as a single JSON-formatted string
         Then Parse it using a JSONObject


 */