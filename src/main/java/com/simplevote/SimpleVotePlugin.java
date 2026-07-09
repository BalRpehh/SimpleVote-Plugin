package com.simplevote;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SimpleVotePlugin extends JavaPlugin {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String apiKey;

    @Override
    public void onEnable() {
        // 1. Buat config.yml jika belum ada di folder server
        saveDefaultConfig();

        // 2. Baca API Key dari config.yml
        apiKey = getConfig().getString("api-key");

        if (apiKey == null || apiKey.equals("ISI_API_KEY_KAMU_DISINI")) {
            getLogger().warning("API Key belum disetting di config.yml! Plugin tidak akan mengecek vote.");
            return;
        }

        getLogger().info("SimpleVote menyala! Menggunakan API Key: " + apiKey.substring(0, 5) + "...");

        // 3. Jalankan pengecekan API setiap 30 detik (600 ticks) di background agar server tidak lag
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkQueue, 100L, 600L);
    }

    private void checkQueue() {
        try {
            // Nembak ke API buatanmu
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:3000/api/plugin/queue"))
                    .header("x-api-key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Jika API membalas dengan sukses (Data ditemukan)
            if (response.statusCode() == 200) {
                JsonArray queue = gson.fromJson(response.body(), JsonArray.class);

                for (int i = 0; i < queue.size(); i++) {
                    JsonObject vote = queue.get(i).getAsJsonObject();
                    String username = vote.get("username").getAsString();
                    String commandTemplate = vote.get("command").getAsString();

                    // Ganti tulisan {player} menjadi nama asli pemainnya
                    String finalCommand = commandTemplate.replace("{player}", username);

                    // Eksekusi perintah (harus dikembalikan ke thread utama/Synchronous)
                    getServer().getScheduler().runTask(this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        getLogger().info("VOTE MASUK! Mengeksekusi hadiah untuk: " + username);
                    });
                }
            }
        } catch (Exception e) {
            getLogger().warning("Gagal menyambung ke API Cloud: " + e.getMessage());
        }
    }
}
