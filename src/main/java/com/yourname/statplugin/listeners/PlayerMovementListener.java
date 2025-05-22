package com.yourname.statplugin.listeners;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatEffectApplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMovementListener implements Listener {

    private final PlayerStatManager playerStatManager;
    private final StatEffectApplier statEffectApplier;

    public PlayerMovementListener(PlayerStatManager playerStatManager, StatEffectApplier statEffectApplier) {
        this.playerStatManager = playerStatManager;
        this.statEffectApplier = statEffectApplier;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Y 좌표 30을 기준으로 상태가 변경되었는지 확인
        boolean wasInDeep = from.getY() < 30;
        boolean isInDeep = to.getY() < 30;

        if (wasInDeep != isInDeep) {
            // Y좌표 상태가 변경되면 광부 조건부 효과 재적용
            statEffectApplier.applyMiningConditionalEffects(player);
        }
    }
}