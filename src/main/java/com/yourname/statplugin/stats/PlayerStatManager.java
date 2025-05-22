package com.yourname.statplugin.stats;

import com.yourname.statplugin.StatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerStatManager {

    private final StatPlugin plugin;
    private final Map<UUID, EnumMap<StatType, Integer>> playerStatsData = new HashMap<>();
    private final Map<UUID, Long> utilityCooldowns = new HashMap<>(); // 유틸리티 스탯 쿨타임 (종료 시각)

    public PlayerStatManager(StatPlugin plugin) {
        this.plugin = plugin;
        // 서버 시작 시 온라인 플레이어가 있다면 로드 (리로드 시나리오)
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
    }

    public int getStatLevel(Player player, StatType type) {
        return playerStatsData.getOrDefault(player.getUniqueId(), new EnumMap<>(StatType.class))
                .getOrDefault(type, 0);
    }

    public void setStatLevel(Player player, StatType type, int level) {
        playerStatsData.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(StatType.class))
                .put(type, Math.max(0, Math.min(level, type.getMaxLevel())));
        // 스탯 변경 시 즉시 효과 적용
        plugin.getStatEffectApplier().applyStatEffects(player, type);
        savePlayerData(player); // 변경 시 즉시 저장
    }

    public void incrementStatLevel(Player player, StatType type) {
        int currentLevel = getStatLevel(player, type);
        if (currentLevel < type.getMaxLevel()) {
            setStatLevel(player, type, currentLevel + 1);
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(plugin.getDataFolder(), "playerdata/" + uuid.toString() + ".yml");
        EnumMap<StatType, Integer> stats = new EnumMap<>(StatType.class);

        if (playerFile.exists()) {
            FileConfiguration playerDataConfig = YamlConfiguration.loadConfiguration(playerFile);
            for (StatType type : StatType.values()) {
                stats.put(type, playerDataConfig.getInt("stats." + type.name(), 0));
            }
            if (playerDataConfig.contains("cooldowns.utility")) {
                utilityCooldowns.put(uuid, playerDataConfig.getLong("cooldowns.utility"));
            }
        } else {
            // 파일이 없으면 모든 스탯을 0으로 초기화
            for (StatType type : StatType.values()) {
                stats.put(type, 0);
            }
        }
        playerStatsData.put(uuid, stats);

        // 스탯 로드 후 모든 효과 재적용 (Attribute Modifier 초기화 등 때문)
        plugin.getStatEffectApplier().applyAllStatEffects(player);
        plugin.getLogger().info(player.getName() + "님의 스탯 정보를 로드했습니다.");
    }

    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId(), player.getName());
    }

    public void savePlayerData(UUID uuid, String playerNameForLog) {
        EnumMap<StatType, Integer> stats = playerStatsData.get(uuid);
        if (stats == null) { // 데이터가 없는 경우 (예: 서버 종료 중 오프라인 플레이어)
            // plugin.getLogger().warning(playerNameForLog + "님의 스탯 데이터가 메모리에 없어 저장할 수 없습니다.");
            return;
        }

        File playerFile = new File(plugin.getDataFolder(), "playerdata/" + uuid.toString() + ".yml");
        FileConfiguration playerDataConfig = YamlConfiguration.loadConfiguration(playerFile); // 기존 파일 로드 또는 새로 생성

        for (Map.Entry<StatType, Integer> entry : stats.entrySet()) {
            playerDataConfig.set("stats." + entry.getKey().name(), entry.getValue());
        }
        if (utilityCooldowns.containsKey(uuid)) {
            playerDataConfig.set("cooldowns.utility", utilityCooldowns.get(uuid));
        } else {
            playerDataConfig.set("cooldowns.utility", null); // 없으면 명시적으로 제거
        }

        try {
            playerDataConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, playerNameForLog + "님의 스탯 정보 저장에 실패했습니다: " + playerFile.getAbsolutePath(), e);
        }
    }

    public void saveAllPlayerData() {
        plugin.getLogger().info("모든 접속 중인 플레이어의 스탯 정보를 저장합니다...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
        // 메모리에 있지만 오프라인인 플레이어 데이터도 저장 시도 (선택적, 일반적으로는 불필요)
        for (UUID uuid : playerStatsData.keySet()) {
            if (Bukkit.getPlayer(uuid) == null) { // 오프라인 플레이어
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                savePlayerData(uuid, offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString());
            }
        }
    }

    public void unloadPlayerData(Player player) {
        // 접속 종료 시 메모리에서 데이터 제거 (선택적, 메모리 관리용)
        // savePlayerData(player); // 이미 PlayerQuitEvent에서 호출됨
        // playerStatsData.remove(player.getUniqueId());
        // utilityCooldowns.remove(player.getUniqueId());
        // plugin.getLogger().info(player.getName() + "님의 스탯 정보를 메모리에서 언로드했습니다.");
    }


    public boolean isUtilityOnCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        long cooldownEnd = utilityCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return currentTime < cooldownEnd;
    }

    public long getUtilityCooldownRemaining(Player player) {
        if (!isUtilityOnCooldown(player)) return 0;
        return (utilityCooldowns.get(player.getUniqueId()) - System.currentTimeMillis() + 999) / 1000; // 올림 초 단위
    }

    public void setUtilityCooldown(Player player) {
        long cooldownMillis = 3 * 60 * 1000; // 3분
        utilityCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMillis);
        savePlayerData(player); // 쿨타임 변경 시 저장
    }
}