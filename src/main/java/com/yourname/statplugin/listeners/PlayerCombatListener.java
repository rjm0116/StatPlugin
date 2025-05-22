package com.yourname.statplugin.listeners;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatEffectApplier;
import com.yourname.statplugin.stats.StatType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class PlayerCombatListener implements Listener {

    private final PlayerStatManager playerStatManager;
    private final StatEffectApplier statEffectApplier; // Y좌표 공격력 적용 위해 필요
    private final Random random = new Random();

    public PlayerCombatListener(PlayerStatManager playerStatManager, StatEffectApplier statEffectApplier) {
        this.playerStatManager = playerStatManager;
        this.statEffectApplier = statEffectApplier;
    }

    @EventHandler(priority = EventPriority.NORMAL) // 다른 플러그인과의 호환성을 위해 HIGHEST는 피하는 편
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // 도박 스탯 (공격)
        if (damager instanceof Player attacker) {
            int gambleLevel = playerStatManager.getStatLevel(attacker, StatType.GAMBLE);
            if (gambleLevel > 0) {
                double chance = (5.0 + (gambleLevel * 0.5)) / 100.0;
                if (random.nextDouble() < chance) {
                    event.setDamage(event.getDamage() * 1.5); // 데미지 50% 증가
                    attacker.sendMessage(ChatColor.GOLD + "🎲 도박 성공! 추가 데미지!");
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                }
            }
        }

        // 도박 스탯 (방어)
        if (victim instanceof Player defender) {
            int gambleLevel = playerStatManager.getStatLevel(defender, StatType.GAMBLE);
            if (gambleLevel > 0) {
                double chance = (5.0 + (gambleLevel * 0.5)) / 100.0;
                if (random.nextDouble() < chance) {
                    event.setCancelled(true); // 데미지 무시
                    defender.sendMessage(ChatColor.GOLD + "🎲 도박 성공! 공격 회피!");
                    defender.playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                    return; // 데미지 무시되었으므로 이하 로직 실행 안 함
                }
            }
        }

        // 광부 스탯 (Y<30 몹 대상 공격력 증가) - 이 부분은 StatEffectApplier에서 AttributeModifier로 관리하는 것이 더 적합함
        // PlayerMovementListener에서 applyMiningConditionalEffects를 호출하여 AttributeModifier를 동적으로 적용/제거
        // 여기서는 해당 AttributeModifier가 적용된 상태로 데미지가 계산됨.
        // 만약 AttributeModifier를 사용하지 않고 여기서 직접 계산하려면 Y좌표 체크가 필요.
        // 하지만 AttributeModifier 방식이 더 깔끔하고, 다른 플러그인과의 호환성(데미지 계산 이벤트 순서)도 좋음.
        // StatEffectApplier의 applyMiningConditionalEffects가 이미 Y좌표 공격력 AttributeModifier를 관리함.
    }
}