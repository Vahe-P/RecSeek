package com.example.recs;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.*;
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    EditText editTextQuery;
    Button buttonGetRecommendations;
    Button chipMovies, chipMusic, chipBooks, chipAnime, chipTV;
    Button buttonFavorites, buttonHistory;
    String selectedType = "movie";
    ListView listViewResults;
    ArrayAdapter<String> resultsAdapter;
    List<String> currentResults = new ArrayList<>();
    Set<String> favorites = new HashSet<>();
    List<String> history = new ArrayList<>();

    OkHttpClient client = new OkHttpClient();
    String serverUrl = "https://003f-34-106-102-241.ngrok-free.app/recommend"; // Replace with your actual URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonGetRecommendations = findViewById(R.id.buttonGetRecommendations);
        chipMovies = findViewById(R.id.chipMovies);
        chipMusic = findViewById(R.id.chipMusic);
        chipBooks = findViewById(R.id.chipBooks);
        chipAnime = findViewById(R.id.chipAnime);
        chipTV = findViewById(R.id.chipTV);
        buttonFavorites = findViewById(R.id.buttonFavorites);
        buttonHistory = findViewById(R.id.buttonHistory);
        listViewResults = findViewById(R.id.listViewResults);

        View.OnClickListener chipListener = v -> {
            resetChips();
            v.setBackgroundTintList(getColorStateList(R.color.purple_accent));
            ((Button)v).setTextColor(getColor(R.color.white));
            if (v == chipMovies) selectedType = "movie";
            else if (v == chipMusic) selectedType = "music";
            else if (v == chipBooks) selectedType = "book";
            else if (v == chipAnime) selectedType = "anime";
            else if (v == chipTV) selectedType = "tvshow";
        };
        chipMovies.setOnClickListener(chipListener);
        chipMusic.setOnClickListener(chipListener);
        chipBooks.setOnClickListener(chipListener);
        chipAnime.setOnClickListener(chipListener);
        chipTV.setOnClickListener(chipListener);
        // Set default selected chip
        chipMovies.performClick();

        buttonGetRecommendations.setOnClickListener(v -> {
            String query = editTextQuery.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show();
                return;
            }
            getRecommendations(selectedType, query);
        });

        resultsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, currentResults) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                String item = getItem(position);
                // Add a star to favorites
                if (favorites.contains(item)) {
                    text.setText("\u2B50 " + item); // Star emoji
                } else {
                    text.setText(item);
                }
                text.setTextColor(getColor(R.color.white));
                return view;
            }
        };
        listViewResults.setAdapter(resultsAdapter);
        listViewResults.setOnItemClickListener((parent, view, position, id) -> {
            String item = currentResults.get(position);
            if (favorites.contains(item)) {
                favorites.remove(item);
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                favorites.add(item);
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }
            resultsAdapter.notifyDataSetChanged();
        });
        buttonFavorites.setOnClickListener(v -> {
            showResults(new ArrayList<>(favorites), "Favorites");
        });
        buttonHistory.setOnClickListener(v -> {
            showResults(history, "History");
        });
    }

    private void resetChips() {
        chipMovies.setBackgroundTintList(getColorStateList(R.color.white));
        chipMusic.setBackgroundTintList(getColorStateList(R.color.white));
        chipBooks.setBackgroundTintList(getColorStateList(R.color.white));
        chipAnime.setBackgroundTintList(getColorStateList(R.color.white));
        chipTV.setBackgroundTintList(getColorStateList(R.color.white));
        chipMovies.setTextColor(getColor(R.color.purple_dark));
        chipMusic.setTextColor(getColor(R.color.purple_dark));
        chipBooks.setTextColor(getColor(R.color.purple_dark));
        chipAnime.setTextColor(getColor(R.color.purple_dark));
        chipTV.setTextColor(getColor(R.color.purple_dark));
    }

    private void getRecommendations(String type, String query) {
        currentResults.clear();
        resultsAdapter.notifyDataSetChanged();
        // Add to history
        history.add(type + ": " + query);
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
                        currentResults.clear();
                        currentResults.add("Connection failed: " + e.getMessage());
                        resultsAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            if (jsonResponse.has("error")) {
                                currentResults.clear();
                                currentResults.add("Error: " + jsonResponse.getString("error"));
                                resultsAdapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "Error: " + jsonResponse.getString("error"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            StringBuilder header = new StringBuilder();
                            JSONArray recommendations = jsonResponse.getJSONArray("recommendations");
                            switch (type) {
                                case "movie":
                                    header.append("Recommended Movies:");
                                    break;
                                case "music":
                                    header.append("Recommended Songs:");
                                    break;
                                case "book":
                                    header.append("Recommended Books:");
                                    break;
                                case "anime":
                                    header.append("Recommended Anime:");
                                    break;
                                case "tvshow":
                                    header.append("Recommended TV Shows:");
                                    break;
                            }
                            currentResults.clear();
                            currentResults.add(header.toString());
                            for (int i = 0; i < recommendations.length(); i++) {
                                String rec;
                                if (type.equals("music")) {
                                    JSONObject song = recommendations.getJSONObject(i);
                                    rec = (i + 1) + ". " + song.getString("Title") + " by " + song.getString("Artist");
                                } else if (type.equals("book")) {
                                    JSONObject book = recommendations.getJSONObject(i);
                                    rec = (i + 1) + ". " + book.getString("title") + " by " + book.getString("authors");
                                } else if (type.equals("tvshow")) {
                                    JSONObject show = recommendations.getJSONObject(i);
                                    rec = (i + 1) + ". " + show.getString("primaryTitle") + " (" + show.getString("genres") + ") ★" + show.getDouble("averageRating");
                                } else {
                                    rec = (i + 1) + ". " + recommendations.getString(i);
                                }
                                currentResults.add(rec);
                            }
                            resultsAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            currentResults.clear();
                            currentResults.add("Error parsing response: " + e.getMessage());
                            resultsAdapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            currentResults.clear();
            currentResults.add("Error creating request");
            resultsAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_LONG).show();
        }
    }

    private void showResults(List<String> list, String title) {
        currentResults.clear();
        if (list.isEmpty()) {
            currentResults.add(title + " is empty.");
        } else {
            currentResults.add(title + ":");
            currentResults.addAll(list);
        }
        resultsAdapter.notifyDataSetChanged();
    }
}