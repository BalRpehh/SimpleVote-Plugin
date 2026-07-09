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
    
    // URL API resmi yang sudah diproteksi Cloudflare dan diarahkan otomatis ke port 3000
    private static final String API_URL = "https://api.iqbalafkbot.my.id";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String apiKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Membaca API Key dari config.yml milik pembeli/user
        this.apiKey = getConfig().getString("api-key");

        if (apiKey == null || apiKey.equals("ISI_API_KEY_KAMU_DISINI")) {
            getLogger().warning("========================================");
            getLogger().warning(" API Key belum diisi! Plugin berhenti.");
            getLogger().warning("========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("SimpleVote menyala!");
        getLogger().info("Terhubung ke server pusat: " + API_URL);

        // Menjalankan pengecekan antrean vote setiap 30 detik (600 tick) secara Async
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkQueue, 100L, 600L);
    }

    private void checkQueue() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/plugin/queue"))
                    .header("x-api-key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray queue = gson.fromJson(response.body(), JsonArray.class);
                
                for (int i = 0; i < queue.size(); i++) {
                    JsonObject vote = queue.get(i).getAsJsonObject();
                    String username = vote.get("username").getAsString();
                    String commandTemplate = vote.get("command").getAsString();

                    // Mengganti placeholder {player} dengan nama asli pemenang vote
                    String finalCommand = commandTemplate.replace("{player}", username);

                    // Mengeksekusi command di thread utama Minecraft (Console Sender)
                    getServer().getScheduler().runTask(this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        getLogger().info("VOTE MASUK! Memberikan hadiah ke: " + username);
                    });
                }
            }
        } catch (Exception e) {
            getLogger().warning("Gagal terhubung ke API: " + e.getMessage());
        }
    }
}
