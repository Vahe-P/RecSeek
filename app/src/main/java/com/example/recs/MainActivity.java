package com.example.recs;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.*;
import okhttp3.*;
import org.json.*;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    EditText editTextQuery;
    Button buttonGetRecommendations;
    TextView textViewResults;
    RadioGroup typeSelector;
    RadioButton movieOption, musicOption, bookOption, animeOption, tvOption;

    OkHttpClient client = new OkHttpClient();
    String serverUrl = "https://YOUR_NGROK_URL/recommend"; // Replace with your actual URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonGetRecommendations = findViewById(R.id.buttonGetRecommendations);
        textViewResults = findViewById(R.id.textViewResults);
        typeSelector = findViewById(R.id.typeSelector);
        movieOption = findViewById(R.id.movieOption);
        musicOption = findViewById(R.id.musicOption);
        bookOption = findViewById(R.id.bookOption);
        animeOption = findViewById(R.id.animeOption);
        tvOption = findViewById(R.id.tvOption);

        buttonGetRecommendations.setOnClickListener(v -> {
            String query = editTextQuery.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = "movie";
            if (musicOption.isChecked()) {
                type = "music";
            } else if (bookOption.isChecked()) {
                type = "book";
            } else if (animeOption.isChecked()) {
                type = "anime";
            } else if (tvOption.isChecked()) {
                type = "tvshow";
            }

            getRecommendations(type, query);
        });
    }

    private void getRecommendations(String type, String query) {
        runOnUiThread(() -> textViewResults.setText("Loading..."));

        try {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("query", query);

            Request request = new Request.Builder()
                    .url(serverUrl)
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        textViewResults.setText("Connection failed: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Check your internet connection", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            if (jsonResponse.has("error")) {
                                textViewResults.setText("Error: " + jsonResponse.getString("error"));
                                return;
                            }

                            StringBuilder result = new StringBuilder();
                            JSONArray recommendations = jsonResponse.getJSONArray("recommendations");

                            switch (type) {
                                case "movie":
                                    result.append("Recommended Movies:\n\n");
                                    for (int i = 0; i < recommendations.length(); i++) {
                                        result.append(i + 1).append(". ").append(recommendations.getString(i)).append("\n");
                                    }
                                    break;
                                case "music":
                                    result.append("Recommended Songs:\n\n");
                                    for (int i = 0; i < recommendations.length(); i++) {
                                        JSONObject song = recommendations.getJSONObject(i);
                                        result.append(i + 1).append(". ")
                                                .append(song.getString("Title"))
                                                .append(" by ")
                                                .append(song.getString("Artist"))
                                                .append("\n");
                                    }
                                    break;
                                case "book":
                                    result.append("Recommended Books:\n\n");
                                    for (int i = 0; i < recommendations.length(); i++) {
                                        JSONObject book = recommendations.getJSONObject(i);
                                        result.append(i + 1).append(". ")
                                                .append(book.getString("title"))
                                                .append(" by ")
                                                .append(book.getString("authors"))
                                                .append("\n");
                                    }
                                    break;
                                case "anime":
                                    result.append("Recommended Anime:\n\n");
                                    for (int i = 0; i < recommendations.length(); i++) {
                                        result.append(i + 1).append(". ").append(recommendations.getString(i)).append("\n");
                                    }
                                    break;
                                case "tvshow":
                                    result.append("Recommended TV Shows:\n\n");
                                    for (int i = 0; i < recommendations.length(); i++) {
                                        JSONObject show = recommendations.getJSONObject(i);
                                        result.append(i + 1).append(". ")
                                                .append(show.getString("primaryTitle"))
                                                .append(" (")
                                                .append(show.getString("genres"))
                                                .append(") ★")
                                                .append(show.getDouble("averageRating"))
                                                .append("\n");
                                    }
                                    break;
                            }

                            textViewResults.setText(result.toString());

                        } catch (JSONException e) {
                            textViewResults.setText("Error parsing response: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (JSONException e) {
            runOnUiThread(() -> textViewResults.setText("Error creating request"));
        }
    }
}