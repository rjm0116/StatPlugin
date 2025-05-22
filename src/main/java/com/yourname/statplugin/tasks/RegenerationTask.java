package com.yourname.statplugin.tasks;

import com.yourname.statplugin.StatPlugin;
import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegenerationTask extends BukkitRunnable {

    private final StatPlugin plugin;
    private final PlayerStatManager playerStatManager;
    private final Map<UUID, Integer> playerRegenCooldownTicks = new HashMap<>(); // 플레이어별 남은 쿨타임 (틱)

    public RegenerationTask(StatPlugin plugin, PlayerStatManager playerStatManager) {
        this.plugin = plugin;
        this.playerStatManager = playerStatManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) {
                playerRegenCooldownTicks.remove(player.getUniqueId());
                continue;
            }

            int regenLevel = playerStatManager.getStatLevel(player, StatType.REGENERATION);
            if (regenLevel == 0) {
                playerRegenCooldownTicks.remove(player.getUniqueId()); // 스탯 없으면 쿨다운 제거
                continue;
            }

            // 재생 쿨타임 계산 (초 단위 -> 틱 단위)
            // 기본 20초, 레벨당 1초 감소, 최소 1초
            int baseCooldownSeconds = 20;
            int cooldownReduction = regenLevel;
            int actualCooldownSeconds = Math.max(1, baseCooldownSeconds - cooldownReduction);
            int actualCooldownTicks = actualCooldownSeconds * 20; // 초를 틱으로 변환

            int currentCooldown = playerRegenCooldownTicks.getOrDefault(player.getUniqueId(), actualCooldownTicks);

            if (currentCooldown <= 0) {
                // 체력 회복
                AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = (maxHealthAttr != null) ? maxHealthAttr.getValue() : 20.0;
                if (player.getHealth() < maxHealth) {
                    player.setHealth(Math.min(maxHealth, player.getHealth() + 2.0)); // 체력 2 회복
                }
                playerRegenCooldownTicks.put(player.getUniqueId(), actualCooldownTicks); // 쿨타임 초기화
            } else {
                playerRegenCooldownTicks.put(player.getUniqueId(), currentCooldown - 20); // 1초(20틱) 감소 (매초 실행되므로)
            }
        }
    }
}