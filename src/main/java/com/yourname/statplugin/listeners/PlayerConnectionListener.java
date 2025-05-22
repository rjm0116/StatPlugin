package com.yourname.statplugin.listeners;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatEffectApplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PlayerStatManager playerStatManager;
    private final StatEffectApplier statEffectApplier;

    public PlayerConnectionListener(PlayerStatManager playerStatManager, StatEffectApplier statEffectApplier) {
        this.playerStatManager = playerStatManager;
        this.statEffectApplier = statEffectApplier;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStatManager.loadPlayerData(player);
        // loadPlayerData 내부에서 applyAllStatEffects가 호출되도록 수정했으므로 여기서는 중복 호출 X
        // statEffectApplier.applyAllStatEffects(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerStatManager.savePlayerData(player);
        statEffectApplier.removeAllAttributeModifiers(player); // 접속 종료 시 모든 Attribute Modifier 제거
        playerStatManager.unloadPlayerData(player); // 메모리에서 데이터 언로드 (선택적)
    }
}