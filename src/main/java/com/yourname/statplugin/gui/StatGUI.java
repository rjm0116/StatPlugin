package com.yourname.statplugin.gui;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatGUI {

    public static final String MAIN_GUI_TITLE = ChatColor.DARK_AQUA + "스탯 트리";
    public static final String STAT_POINT_ITEM_NAME = ChatColor.GREEN + "스탯 포인트"; // 내구성1 에메랄드

    // 메인 스탯 선택 GUI
    public static void openMainStatGUI(Player player, PlayerStatManager statManager) {
        Inventory gui = Bukkit.createInventory(null, 27, MAIN_GUI_TITLE); // 3x9 크기

        // 중앙 정렬을 위한 시작 슬롯 계산
        int numStats = StatType.values().length;
        int guiCenter = 13; // 27칸 GUI의 중앙 슬롯
        int startSlot = guiCenter - (numStats / 2) * 2 + (numStats % 2 == 0 ? 1 : 0); // 짝수, 홀수 아이템 개수 대응
        if (numStats > 5) startSlot = 10; // 너무 많으면 10번 슬롯부터 시작

        int currentSlot = startSlot;
        for (StatType type : StatType.values()) {
            if (currentSlot == 17 && numStats > 4) currentSlot = 19; // 한 줄 넘기기 (UI 개선 여지)

            ItemStack item = new ItemStack(type.getIconMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(type.getGuiTitle()); // Enum에 정의된 GUI용 제목 사용
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "클릭하여 " + type.getDisplayName() + " 스탯을 확인/강화합니다.");
                lore.add("");
                int currentLevel = statManager.getStatLevel(player, type);
                lore.add(ChatColor.YELLOW + "현재 레벨: " + ChatColor.GOLD + currentLevel + "/" + type.getMaxLevel());
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            gui.setItem(currentSlot, item);
            currentSlot += (numStats <= 5 && currentSlot % 9 < 7) ? 2 : 1; // 간격 조절
            if (currentSlot > 16 && currentSlot < 19 && numStats > 5) currentSlot = 19; // 다음 줄로
        }
        player.openInventory(gui);
    }

    // 개별 스탯 강화 GUI
    public static void openStatUpgradeGUI(Player player, PlayerStatManager statManager, StatType type) {
        Inventory gui = Bukkit.createInventory(null, 27, type.getGuiTitle());

        int currentLevel = statManager.getStatLevel(player, type);
        int maxLevel = type.getMaxLevel();

        // 스탯 정보 아이템 (중앙 13번 슬롯)
        ItemStack infoItem = new ItemStack(type.getIconMaterial());
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.BOLD + "" + ChatColor.YELLOW + type.getDisplayName() + " 정보");
            List<String> lore = new ArrayList<>(Arrays.asList(type.getDescription())); // 상세 설명 먼저
            lore.add("");
            lore.add(ChatColor.GOLD + "현재 레벨: " + currentLevel + "/" + maxLevel);
            addCurrentEffectLore(lore, player, statManager, type, currentLevel); // 현재 효과
            infoMeta.setLore(lore);
            infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(13, infoItem);

        // 업그레이드 버튼 (왼쪽 11번 슬롯)
        if (currentLevel < maxLevel) {
            ItemStack upgradeItem = new ItemStack(Material.EMERALD);
            ItemMeta upgradeMeta = upgradeItem.getItemMeta();
            if (upgradeMeta != null) {
                upgradeMeta.setDisplayName(ChatColor.GREEN + "스탯 업그레이드");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "다음 레벨: " + ChatColor.YELLOW + (currentLevel + 1));
                lore.add(ChatColor.GRAY + "필요: " + STAT_POINT_ITEM_NAME + ChatColor.GRAY + " 1개");
                addNextLevelEffectLore(lore, player, statManager, type, currentLevel + 1); // 다음 레벨 효과
                upgradeMeta.setLore(lore);
                upgradeItem.setItemMeta(upgradeMeta);
            }
            gui.setItem(11, upgradeItem);
        } else {
            ItemStack maxLevelItem = new ItemStack(Material.BARRIER);
            ItemMeta maxMeta = maxLevelItem.getItemMeta();
            if (maxMeta != null) {
                maxMeta.setDisplayName(ChatColor.RED + "최대 레벨 달성");
                maxLevelItem.setItemMeta(maxMeta);
            }
            gui.setItem(11, maxLevelItem);
        }

        // 유틸리티 스탯 "사용하기" 버튼 (오른쪽 15번 슬롯)
        if (type == StatType.UTILITY && currentLevel > 0) {
            ItemStack useUtilityItem = new ItemStack(Material.DIAMOND_BLOCK); // 다이아 소모 암시
            ItemMeta useMeta = useUtilityItem.getItemMeta();
            if (useMeta != null) {
                useMeta.setDisplayName(ChatColor.AQUA + "유틸리티 스킬 사용");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "다이아몬드 1개를 소모하여 랜덤 버프를 받습니다.");
                lore.add(ChatColor.GRAY + "쿨타임: 3분");
                int utilityLevel = statManager.getStatLevel(player, StatType.UTILITY);
                int duration = 120 + (utilityLevel * 10);
                lore.add(ChatColor.GRAY + "버프 지속 시간: " + (duration / 60) + "분 " + (duration % 60) + "초");

                if (statManager.isUtilityOnCooldown(player)) {
                    lore.add(ChatColor.RED + "남은 쿨타임: " + statManager.getUtilityCooldownRemaining(player) + "초");
                } else {
                    lore.add(ChatColor.GREEN + "사용 가능");
                }
                useMeta.setLore(lore);
                useUtilityItem.setItemMeta(useMeta);
            }
            gui.setItem(15, useUtilityItem);
        }

        // 뒤로가기 버튼 (하단 왼쪽 18번 슬롯)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.YELLOW + "뒤로 가기");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(18, backButton);

        player.openInventory(gui);
    }

    private static void addCurrentEffectLore(List<String> lore, Player player, PlayerStatManager manager, StatType type, int level) {
        if (level == 0) return;
        lore.add("");
        lore.add(ChatColor.AQUA + "=== 현재 적용 효과 ===");
        switch (type) {
            case HEALTH:
                lore.add(ChatColor.GRAY + "최대 체력 증가: +" + ChatColor.GREEN + (level * 2));
                break;
            case GAMBLE:
                double gambleChance = 5.0 + (level * 0.5);
                lore.add(ChatColor.GRAY + "공격 시 데미지 50% 증가 확률: " + ChatColor.GREEN + String.format("%.1f%%", gambleChance));
                lore.add(ChatColor.GRAY + "방어 시 데미지 무시 확률: " + ChatColor.GREEN + String.format("%.1f%%", gambleChance));
                break;
            case SPEED:
                lore.add(ChatColor.GRAY + "이동 속도 증가: +" + ChatColor.GREEN + String.format("%.1f%%", level * 1.5));
                break;
            case MINING:
                lore.add(ChatColor.GRAY + "상시 채굴 속도 증가: +" + ChatColor.GREEN + (level * 2) + "%");
                lore.add(ChatColor.GRAY + "Y<30 몹 대상 공격력 증가: +" + ChatColor.GREEN + String.format("%.1f%%", level * 1.5));
                if (level / 5 > 0) {
                    lore.add(ChatColor.GRAY + "Y<30 최대 체력 증가 (5레벨당): +" + ChatColor.GREEN + ((level / 5) * 4));
                }
                break;
            case REGENERATION:
                int regenCooldown = Math.max(1, 20 - level);
                lore.add(ChatColor.GRAY + "체력 " + ChatColor.GREEN + "2" + ChatColor.GRAY + " 회복 / " + ChatColor.GREEN + regenCooldown + "초");
                break;
            case UTILITY:
                int duration = 120 + (level * 10);
                lore.add(ChatColor.GRAY + "사용 시 버프 지속 시간: " + ChatColor.GREEN + (duration / 60) + "분 " + (duration % 60) + "초");
                break;
        }
    }

    private static void addNextLevelEffectLore(List<String> lore, Player player, PlayerStatManager manager, StatType type, int nextLevel) {
        if (nextLevel > type.getMaxLevel()) return;
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "--- 다음 레벨 효과 (" + nextLevel + ") ---");
        switch (type) {
            case HEALTH:
                lore.add(ChatColor.GRAY + "최대 체력 증가: +" + ChatColor.GREEN + (nextLevel * 2));
                break;
            case GAMBLE:
                double gambleChance = 5.0 + (nextLevel * 0.5);
                lore.add(ChatColor.GRAY + "공격 성공률: " + ChatColor.GREEN + String.format("%.1f%%", gambleChance));
                lore.add(ChatColor.GRAY + "방어 성공률: " + ChatColor.GREEN + String.format("%.1f%%", gambleChance));
                break;
            case SPEED:
                lore.add(ChatColor.GRAY + "이동 속도 증가: +" + ChatColor.GREEN + String.format("%.1f%%", nextLevel * 1.5));
                break;
            case MINING:
                lore.add(ChatColor.GRAY + "상시 채굴 속도 증가: +" + ChatColor.GREEN + (nextLevel * 2) + "%");
                lore.add(ChatColor.GRAY + "Y<30 몹 대상 공격력 증가: +" + ChatColor.GREEN + String.format("%.1f%%", nextLevel * 1.5));
                if (nextLevel / 5 > (nextLevel-1)/5 && nextLevel % 5 == 0) { // 5레벨 단위로 체력이 오르는 시점
                    lore.add(ChatColor.GRAY + "Y<30 최대 체력 증가: +" + ChatColor.GREEN + ((nextLevel / 5) * 4));
                }
                break;
            case REGENERATION:
                int regenCooldown = Math.max(1, 20 - nextLevel);
                lore.add(ChatColor.GRAY + "체력 " + ChatColor.GREEN + "2" + ChatColor.GRAY + " 회복 / " + ChatColor.GREEN + regenCooldown + "초");
                break;
            case UTILITY:
                int duration = 120 + (nextLevel * 10);
                lore.add(ChatColor.GRAY + "사용 시 버프 지속 시간: " + ChatColor.GREEN + (duration / 60) + "분 " + (duration % 60) + "초");
                break;
        }
    }

    public static ItemStack createStatPointItem(int amount) {
        ItemStack statPoint = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = statPoint.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(STAT_POINT_ITEM_NAME);
            meta.setLore(List.of(ChatColor.GRAY + "스탯을 강화하는 데 사용됩니다."));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true); // 내구성 1 인챈트
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // 인챈트 효과 숨기기
            // meta.setCustomModelData(12345); // 리소스팩 사용 시 커스텀 모델 데이터
        }
        statPoint.setItemMeta(meta);
        return statPoint;
    }

    public static boolean isStatPointItem(ItemStack item) {
        if (item == null || item.getType() != Material.EMERALD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals(STAT_POINT_ITEM_NAME) &&
                meta.hasEnchant(Enchantment.UNBREAKING) &&
                meta.getEnchantLevel(Enchantment.UNBREAKING) == 1;
    }
}