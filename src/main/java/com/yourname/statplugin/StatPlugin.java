package com.yourname.statplugin;

import com.yourname.statplugin.commands.StatTreeCommand;
import com.yourname.statplugin.gui.GUIClickListener;
import com.yourname.statplugin.listeners.*;
import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatEffectApplier;
import com.yourname.statplugin.tasks.RegenerationTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class StatPlugin extends JavaPlugin {

    private static StatPlugin instance;
    private PlayerStatManager playerStatManager;
    private StatEffectApplier statEffectApplier;

    @Override
    public void onEnable() {
        instance = this;

        // 설정 파일 (config.yml)이 없다면 기본값으로 생성
        saveDefaultConfig();

        // 플레이어 데이터 저장 폴더 생성
        File playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        this.playerStatManager = new PlayerStatManager(this);
        this.statEffectApplier = new StatEffectApplier(this, playerStatManager);

        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("StatPlugin이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        if (playerStatManager != null) {
            playerStatManager.saveAllPlayerData(); // 모든 플레이어 데이터 저장
        }
        getLogger().info("StatPlugin이 비활성화되었습니다.");
    }

    private void registerCommands() {
        getCommand("stattree").setExecutor(new StatTreeCommand(playerStatManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIClickListener(playerStatManager, statEffectApplier), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerStatManager, statEffectApplier), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(playerStatManager, statEffectApplier), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(playerStatManager, statEffectApplier), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(playerStatManager), this);
    }

    private void startTasks() {
        new RegenerationTask(this, playerStatManager).runTaskTimer(this, 0L, 20L); // 1초마다 실행 (내부에서 쿨타임 조절)
    }

    public static StatPlugin getInstance() {
        return instance;
    }

    public PlayerStatManager getPlayerStatManager() {
        return playerStatManager;
    }

    public StatEffectApplier getStatEffectApplier() {
        return statEffectApplier;
    }
}