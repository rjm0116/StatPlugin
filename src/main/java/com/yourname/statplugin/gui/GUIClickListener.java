package com.yourname.statplugin.gui;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatEffectApplier;
import com.yourname.statplugin.stats.StatType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIClickListener implements Listener {

    private final PlayerStatManager playerStatManager;
    private final StatEffectApplier statEffectApplier;

    public GUIClickListener(PlayerStatManager playerStatManager, StatEffectApplier statEffectApplier) {
        this.playerStatManager = playerStatManager;
        this.statEffectApplier = statEffectApplier;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(StatGUI.MAIN_GUI_TITLE)) {
            event.setCancelled(true); // 메인 GUI에서는 아이템 이동 방지
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String clickedItemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                for (StatType type : StatType.values()) {
                    // Enum의 guiTitle은 색상 코드가 포함되어 있으므로 stripColor 후 비교
                    if (ChatColor.stripColor(type.getGuiTitle()).trim().equalsIgnoreCase(clickedItemName.trim())) {
                        StatGUI.openStatUpgradeGUI(player, playerStatManager, type);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        return;
                    }
                }
            }
        } else {
            // 개별 스탯 강화 GUI인지 확인
            for (StatType type : StatType.values()) {
                if (inventoryTitle.equals(type.getGuiTitle())) {
                    event.setCancelled(true); // 스탯 강화 GUI에서도 아이템 이동 방지
                    handleStatUpgradeGUIClick(player, event.getCurrentItem(), type);
                    return;
                }
            }
        }
    }

    private void handleStatUpgradeGUIClick(Player player, ItemStack clickedItem, StatType statType) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (itemName.equalsIgnoreCase("뒤로 가기")) {
            StatGUI.openMainStatGUI(player, playerStatManager);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        } else if (itemName.equalsIgnoreCase("스탯 업그레이드")) {
            int currentLevel = playerStatManager.getStatLevel(player, statType);
            if (currentLevel < statType.getMaxLevel()) {
                if (consumeStatPoint(player)) {
                    playerStatManager.incrementStatLevel(player, statType); // 내부에서 효과 적용 호출됨
                    player.sendMessage(ChatColor.GREEN + statType.getDisplayName() + " 스탯이 " +
                            playerStatManager.getStatLevel(player, statType) + "레벨로 상승했습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    StatGUI.openStatUpgradeGUI(player, playerStatManager, statType); // GUI 갱신
                } else {
                    player.sendMessage(ChatColor.RED + StatGUI.STAT_POINT_ITEM_NAME + ChatColor.RED + "이(가) 부족합니다.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "이미 " + statType.getDisplayName() + " 스탯이 최대 레벨입니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.5f);
            }
        } else if (itemName.equalsIgnoreCase("유틸리티 스킬 사용") && statType == StatType.UTILITY) {
            if (playerStatManager.isUtilityOnCooldown(player)) {
                player.sendMessage(ChatColor.RED + "유틸리티 스킬 쿨타임입니다. (" +
                        playerStatManager.getUtilityCooldownRemaining(player) + "초 남음)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            if (player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), 1)) {
                player.getInventory().removeItem(new ItemStack(Material.DIAMOND, 1));
                statEffectApplier.applyUtilityBuff(player); // 유틸리티 버프 적용
                playerStatManager.setUtilityCooldown(player);
                player.sendMessage(ChatColor.AQUA + "유틸리티 스킬을 사용했습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f); // 다른 소리 사용 가능
                StatGUI.openStatUpgradeGUI(player, playerStatManager, statType); // GUI 갱신 (쿨타임 표시)
            } else {
                player.sendMessage(ChatColor.RED + "스킬 사용에 다이아몬드 1개가 부족합니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private boolean consumeStatPoint(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (StatGUI.isStatPointItem(contents[i])) {
                ItemStack statPointItem = contents[i];
                statPointItem.setAmount(statPointItem.getAmount() - 1);
                if (statPointItem.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                } else {
                    player.getInventory().setItem(i, statPointItem);
                }
                player.updateInventory();
                return true;
            }
        }
        return false;
    }
}