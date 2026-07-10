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

    // URL API resmi yang mengarah ke backend Node.js via Cloudflare
    private static final String API_URL = "https://api.iqbalafkbot.my.id";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();    
    private final Gson gson = new Gson();
    private String apiKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Membaca API Key (License) dari config.yml milik pembeli
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
            // Menembak endpoint premium dengan menyertakan x-api-key di Header
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/plugin/queue"))
                    .header("x-api-key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Jika API Key valid dan server merespon 200 OK
            if (response.statusCode() == 200) {
                JsonArray queue = gson.fromJson(response.body(), JsonArray.class);
                
                for (int i = 0; i < queue.size(); i++) {
                    JsonObject vote = queue.get(i).getAsJsonObject();                    
                    String username = vote.get("username").getAsString();
                    
                    // Mengambil perintah yang SUDAH di-replace otomatis oleh Backend Node.js
                    String finalCommand = vote.get("command").getAsString();

                    // Mengeksekusi command langsung di thread utama Minecraft (Console Sender)
                    getServer().getScheduler().runTask(this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        getLogger().info("VOTE MASUK! Memberikan hadiah ke: " + username);
                    });
                }
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                getLogger().severe("========================================");
                getLogger().severe(" API KEY / LISENSI SIMPLEVOTE INVALID! ");
                getLogger().severe(" Plugin otomatis dimatikan oleh server pusat.");
                getLogger().severe("========================================");
                
                // Matikan plugin di thread utama jika lisensi terdeteksi ilegal/expired
                getServer().getScheduler().runTask(this, () -> {
                    getServer().getPluginManager().disablePlugin(this);
                });
            }
        } catch (Exception e) {
            getLogger().warning("Gagal terhubung ke API: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleVote dimatikan.");
    }
}
