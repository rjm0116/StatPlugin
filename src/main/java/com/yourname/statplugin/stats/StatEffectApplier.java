package com.yourname.statplugin.stats;

import com.yourname.statplugin.StatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StatEffectApplier {

    private final StatPlugin plugin;
    private final PlayerStatManager statManager;
    // 플레이어 UUID별로, 어떤 Attribute에 어떤 이름의 Modifier가 적용되었는지 추적
    private final Map<UUID, Map<Attribute, List<AttributeModifier>>> playerModifiers = new HashMap<>();
    // 광부 Y좌표 조건부 체력 모디파이어 추적
    private final Map<UUID, AttributeModifier> miningYHealthModifiers = new HashMap<>();
    // 광부 Y좌표 조건부 공격력 모디파이어 추적
    private final Map<UUID, AttributeModifier> miningYAttackModifiers = new HashMap<>();


    public StatEffectApplier(StatPlugin plugin, PlayerStatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    public void applyAllStatEffects(Player player) {
        // 기존 모든 Attribute Modifier 제거 후 재적용 (가장 확실한 방법)
        removeAllAttributeModifiers(player);
        for (StatType type : StatType.values()) {
            applyStatEffects(player, type);
        }
        // Y좌표 조건부 효과도 현재 위치 기준으로 재적용
        applyMiningConditionalEffects(player);
    }

    public void applyStatEffects(Player player, StatType type) {
        int level = statManager.getStatLevel(player, type);
        UUID playerUUID = player.getUniqueId();

        // 해당 스탯 타입에 대한 기존 Modifier 제거
        removeSpecificAttributeModifier(player, "statplugin_" + type.name().toLowerCase());

        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance speedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        // AttributeInstance attackDamageAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        if (healthAttribute == null || speedAttribute == null) {
            plugin.getLogger().warning(player.getName() + "님의 Attribute 인스턴스를 찾을 수 없습니다.");
            return;
        }

        Map<Attribute, List<AttributeModifier>> modifiersForPlayer =
                playerModifiers.computeIfAbsent(playerUUID, k -> new EnumMap<>(Attribute.class));

        switch (type) {
            case HEALTH:
                if (level > 0) {
                    double healthBonus = level * 2.0;
                    AttributeModifier healthMod = new AttributeModifier(
                            UUID.randomUUID(), // 각 모디파이어는 고유 UUID 필요
                            "statplugin_health", // 이름으로 그룹화하여 제거 용이
                            healthBonus,
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                    healthAttribute.addModifier(healthMod);
                    modifiersForPlayer.computeIfAbsent(Attribute.GENERIC_MAX_HEALTH, k -> new ArrayList<>()).add(healthMod);
                }
                break;
            case SPEED:
                if (level > 0) {
                    double speedBonusPercentage = level * 0.015; // 1.5% = 0.015
                    AttributeModifier speedMod = new AttributeModifier(
                            UUID.randomUUID(),
                            "statplugin_speed",
                            speedBonusPercentage,
                            AttributeModifier.Operation.MULTIPLY_SCALAR_1 // (기본값 + ADD_NUMBER합) * (1 + 이 값)
                    );
                    speedAttribute.addModifier(speedMod);
                    modifiersForPlayer.computeIfAbsent(Attribute.GENERIC_MOVEMENT_SPEED, k -> new ArrayList<>()).add(speedMod);
                }
                break;
            case MINING:
                // 채굴 속도는 BlockBreakListener에서 Haste 효과로 처리
                // Y좌표 조건부 효과는 PlayerMovementListener와 PlayerCombatListener에서 applyMiningConditionalEffects 호출
                applyMiningConditionalEffects(player); // 레벨 변경 시 즉시 재확인
                break;
            // GAMBLE, REGENERATION, UTILITY는 이벤트 리스너나 태스크에서 직접 처리
            default:
                break;
        }
    }

    // 특정 이름 패턴을 가진 Attribute Modifier 제거
    private void removeSpecificAttributeModifier(Player player, String namePrefix) {
        Map<Attribute, List<AttributeModifier>> currentPlayerMods = playerModifiers.get(player.getUniqueId());
        if (currentPlayerMods == null) return;

        for (Attribute attributeType : Attribute.values()) { // 모든 Attribute 타입에 대해 검사
            AttributeInstance pAttribute = player.getAttribute(attributeType);
            if (pAttribute == null) continue;

            List<AttributeModifier> modsOfType = currentPlayerMods.get(attributeType);
            if (modsOfType == null) continue;

            Iterator<AttributeModifier> iterator = modsOfType.iterator();
            while (iterator.hasNext()) {
                AttributeModifier mod = iterator.next();
                if (mod.getName().startsWith(namePrefix)) {
                    try {
                        pAttribute.removeModifier(mod); // Paper에서는 UUID로 제거하는 것이 더 안전할 수 있음
                    } catch (IllegalArgumentException e) {
                        // 이미 제거되었거나 다른 이유로 오류 발생 가능성
                        // plugin.getLogger().warning("Modifier 제거 시도 중 오류 (이미 제거됨?): " + mod.getName());
                    }
                    iterator.remove();
                }
            }
            if(modsOfType.isEmpty()) {
                currentPlayerMods.remove(attributeType);
            }
        }
        if(currentPlayerMods.isEmpty()){
            playerModifiers.remove(player.getUniqueId());
        }
    }


    // 플레이어의 모든 플러그인 Attribute Modifier 제거 (주로 접속 종료 또는 전체 재적용 시)
    public void removeAllAttributeModifiers(Player player) {
        Map<Attribute, List<AttributeModifier>> modMap = playerModifiers.remove(player.getUniqueId());
        if (modMap != null) {
            for (Map.Entry<Attribute, List<AttributeModifier>> entry : modMap.entrySet()) {
                AttributeInstance pAttribute = player.getAttribute(entry.getKey());
                if (pAttribute != null) {
                    for (AttributeModifier modifier : entry.getValue()) {
                        try {
                            pAttribute.removeModifier(modifier);
                        } catch (IllegalArgumentException e) {
                            // 이미 제거됨
                        }
                    }
                }
            }
        }
        // 조건부 모디파이어도 제거
        AttributeModifier miningHealthMod = miningYHealthModifiers.remove(player.getUniqueId());
        if (miningHealthMod != null) {
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) try { healthAttr.removeModifier(miningHealthMod); } catch (IllegalArgumentException e) {}
        }
        AttributeModifier miningAttackMod = miningYAttackModifiers.remove(player.getUniqueId());
        if (miningAttackMod != null) {
            AttributeInstance attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackAttr != null) try { attackAttr.removeModifier(miningAttackMod); } catch (IllegalArgumentException e) {}
        }
        // plugin.getLogger().info(player.getName() + "님의 모든 스탯 Attribute Modifiers를 제거했습니다.");
    }


    // 광부 스탯 Y좌표 조건부 효과 (체력, 공격력) 적용/해제
    public void applyMiningConditionalEffects(Player player) {
        int miningLevel = statManager.getStatLevel(player, StatType.MINING);
        UUID playerUUID = player.getUniqueId();
        boolean isInDeep = player.getLocation().getY() < 30;

        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance attackAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        // 이전 Y좌표 체력 모디파이어 제거
        AttributeModifier oldMiningHealthMod = miningYHealthModifiers.remove(playerUUID);
        if (oldMiningHealthMod != null && healthAttribute != null) {
            try { healthAttribute.removeModifier(oldMiningHealthMod); } catch (IllegalArgumentException e) {}
        }
        // 이전 Y좌표 공격력 모디파이어 제거
        AttributeModifier oldMiningAttackMod = miningYAttackModifiers.remove(playerUUID);
        if (oldMiningAttackMod != null && attackAttribute != null) {
            try { attackAttribute.removeModifier(oldMiningAttackMod); } catch (IllegalArgumentException e) {}
        }

        if (miningLevel > 0 && isInDeep) {
            // 체력 적용 (5레벨당 +4)
            int healthBonusLevels = miningLevel / 5;
            if (healthBonusLevels > 0 && healthAttribute != null) {
                double healthBonus = healthBonusLevels * 4.0;
                AttributeModifier newMiningHealthMod = new AttributeModifier(
                        UUID.randomUUID(), "statplugin_mining_y_health", healthBonus, AttributeModifier.Operation.ADD_NUMBER);
                healthAttribute.addModifier(newMiningHealthMod);
                miningYHealthModifiers.put(playerUUID, newMiningHealthMod);
            }

            // 공격력 적용 (레벨당 +1.5%)
            if (attackAttribute != null) {
                double attackBonusPercentage = miningLevel * 0.015;
                AttributeModifier newMiningAttackMod = new AttributeModifier(
                        UUID.randomUUID(), "statplugin_mining_y_attack", attackBonusPercentage, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                attackAttribute.addModifier(newMiningAttackMod);
                miningYAttackModifiers.put(playerUUID, newMiningAttackMod);
            }
        }
    }


    public void applyUtilityBuff(Player player) {
        int utilityLevel = statManager.getStatLevel(player, StatType.UTILITY);
        if (utilityLevel == 0) return;

        int durationTicks = (120 + (utilityLevel * 10)) * 20; // 기본 2분 + 레벨당 10초 (틱 단위)

        Random random = new Random();
        int chance = random.nextInt(100); // 0-99

        PotionEffectType effectType = null;
        int amplifier = 0;
        String effectName = "";

        if (chance < 25) { // 느린 낙하 (25%)
            effectType = PotionEffectType.SLOW_FALLING; effectName = "느린 낙하";
        } else if (chance < 25 + 15) { // 신속1 (15%)
            effectType = PotionEffectType.SPEED; amplifier = 0; effectName = "신속 I";
        } else if (chance < 25 + 15 + 10) { // 재생1 (10%)
            effectType = PotionEffectType.REGENERATION; amplifier = 0; effectName = "재생 I";
        } else if (chance < 25 + 15 + 10 + 10) { // 흡수2 (10%)
            effectType = PotionEffectType.ABSORPTION; amplifier = 1; effectName = "흡수 II";
        } else if (chance < 25 + 15 + 10 + 10 + 10) { // 야간 투시 (10%)
            effectType = PotionEffectType.NIGHT_VISION; effectName = "야간 투시";
        } else if (chance < 25 + 15 + 10 + 10 + 10 + 8) { // 힘1 (8%)
            effectType = PotionEffectType.STRENGTH; amplifier = 0; effectName = "힘 I";
        } else if (chance < 25 + 15 + 10 + 10 + 10 + 8 + 7) { // 저항1 (7%)
            effectType = PotionEffectType.RESISTANCE; amplifier = 0; effectName = "저항 I";
        } else { // 재생2 (5%) - 나머지 확률
            effectType = PotionEffectType.REGENERATION; amplifier = 1; effectName = "재생 II";
        }

        if (effectType != null) {
            // 기존 동일 타입 효과 제거 후 적용 (중첩 방지)
            player.removePotionEffect(effectType);
            player.addPotionEffect(new PotionEffect(effectType, durationTicks, amplifier, true, true));
            player.sendMessage(ChatColor.GREEN + "유틸리티 효과: " + ChatColor.AQUA + effectName +
                    ChatColor.GREEN + " (" + (durationTicks/20) + "초)");
        }
    }
}