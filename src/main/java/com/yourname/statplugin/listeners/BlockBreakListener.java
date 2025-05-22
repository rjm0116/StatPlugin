package com.yourname.statplugin.listeners;

import com.yourname.statplugin.stats.PlayerStatManager;
import com.yourname.statplugin.stats.StatType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlockBreakListener implements Listener {

    private final PlayerStatManager playerStatManager;

    public BlockBreakListener(PlayerStatManager playerStatManager) {
        this.playerStatManager = playerStatManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        int miningLevel = playerStatManager.getStatLevel(player, StatType.MINING);

        if (miningLevel > 0) {
            // 채굴 속도 2%/레벨 증가 -> Haste 효과로 변환
            // Haste 1 (amplifier 0) = 20% 증가, Haste 2 (amplifier 1) = 40% 증가 ...
            // 2%는 매우 미미하므로, Haste 효과를 주려면 어느정도 레벨이 쌓여야 의미가 있음.
            // 예: 5레벨당 Haste 1 (amplifier 0), 10레벨당 Haste 2 (amplifier 1)
            // 또는 (레벨 * 2 / 20) - 1 을 amplifier로 사용 (소수점 버림)
            // 여기서는 간단하게 레벨당 Haste 효과를 중첩하지 않고,
            // 일정 레벨 도달 시 더 높은 단계의 Haste를 짧게 부여하는 방식 고려.
            // 또는 (레벨 * 0.02) 만큼의 Haste amplifier를 계산. (Haste는 정수형 amplifier)

            int hasteAmplifier = (miningLevel * 2) / 20; // 20%당 Haste 1단계로 가정 (2%는 Haste 0.1)
            // 좀 더 세밀한 조정이 필요함.
            // 예를 들어, 5레벨당 amplifier 0, 10레벨당 1 ...
            hasteAmplifier = Math.max(0, hasteAmplifier -1); // 0레벨부터 시작하게 조정

            // 임시 Haste 효과 부여 (2초 지속). 중첩되지 않도록 기존 효과 제거 후 적용
            if (hasteAmplifier >= 0 && player.hasPotionEffect(PotionEffectType.HASTE)) {
                if(player.getPotionEffect(PotionEffectType.HASTE).getAmplifier() < hasteAmplifier) {
                    player.removePotionEffect(PotionEffectType.HASTE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, hasteAmplifier, true, false, true));
                }
            } else if (hasteAmplifier >=0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, hasteAmplifier, true, false, true)); // 2초 지속
            }


            // 만약 PotionEffect 방식이 마음에 안든다면,
            // Paper API의 Player#breakBlock(Block)를 사용하거나,
            // 이벤트의 setDropItems(false) 후 직접 아이템을 드랍하고 블록을 더 빨리 파괴하는 복잡한 로직 필요.
            // Haste 효과가 가장 간편하고 일반적입니다.
        }
    }
}