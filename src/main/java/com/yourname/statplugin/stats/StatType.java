package com.yourname.statplugin.stats;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum StatType {
    HEALTH("체력", Material.APPLE, 15, ChatColor.RED + "❤️ 체력 강화", new String[]{
            ChatColor.GRAY + "스탯 1개당 최대 체력이 " + ChatColor.GREEN + "2" + ChatColor.GRAY + "만큼 영구적으로 증가합니다."
    }),
    GAMBLE("도박", Material.GOLD_NUGGET, 15, ChatColor.GOLD + "🎲 도박 강화", new String[]{
            ChatColor.GRAY + "공격 시 " + ChatColor.GREEN + "5% (+0.5%/레벨)" + ChatColor.GRAY + " 확률로 데미지 " + ChatColor.GREEN + "50%" + ChatColor.GRAY + " 증가.",
            ChatColor.GRAY + "피격 시 " + ChatColor.GREEN + "5% (+0.5%/레벨)" + ChatColor.GRAY + " 확률로 데미지 무시."
    }),
    SPEED("이동 속도", Material.FEATHER, 15, ChatColor.AQUA + "👟 이동 속도 강화", new String[]{
            ChatColor.GRAY + "스탯 1개당 이동 속도가 " + ChatColor.GREEN + "1.5%" + ChatColor.GRAY + "만큼 영구적으로 증가합니다."
    }),
    MINING("채굴", Material.DIAMOND_PICKAXE, 15, ChatColor.BLUE + "⛏️ 채굴 강화", new String[]{
            ChatColor.YELLOW + "[조건부] " + ChatColor.GRAY + "Y좌표 30 미만:",
            ChatColor.GRAY + "  - 몹 대상 공격력 " + ChatColor.GREEN + "1.5%/레벨" + ChatColor.GRAY + " 증가.",
            ChatColor.GRAY + "  - 5레벨마다 최대 체력 " + ChatColor.GREEN + "4" + ChatColor.GRAY + " 증가.",
            ChatColor.YELLOW + "[상시] " + ChatColor.GRAY + "채굴 속도 " + ChatColor.GREEN + "2%/레벨" + ChatColor.GRAY + " 증가."
    }),
    REGENERATION("재생", Material.GHAST_TEAR, 15, ChatColor.LIGHT_PURPLE + "💖 재생 강화", new String[]{
            ChatColor.GRAY + "기본 " + ChatColor.GREEN + "20초" + ChatColor.GRAY + "마다 체력 " + ChatColor.GREEN + "2" + ChatColor.GRAY + " 회복.",
            ChatColor.GRAY + "단계별로 회복 쿨타임 " + ChatColor.GREEN + "1초" + ChatColor.GRAY + "씩 감소 (최소 1초)."
    }),
    UTILITY("유틸리티", Material.BEACON, 15, ChatColor.GREEN + "🛠️ 유틸리티 강화", new String[]{
            ChatColor.GRAY + "다이아몬드 1개를 소모하여 랜덤 버프 획득 (쿨타임 3분).",
            ChatColor.GRAY + "기본 지속 시간 " + ChatColor.GREEN + "2분" + ChatColor.GRAY + ", 단계별 " + ChatColor.GREEN + "10초" + ChatColor.GRAY + " 증가.",
            ChatColor.DARK_GRAY + "버프 종류: 느린낙하(25%), 신속I(15%), 재생I(10%),",
            ChatColor.DARK_GRAY + "흡수II(10%), 야간투시(10%), 힘I(8%), 저항I(7%), 재생II(5%)"
    });

    private final String displayName;
    private final Material iconMaterial;
    private final int maxLevel;
    private final String guiTitle; // GUI 창 제목 및 메인 GUI 아이템 이름으로 사용될 수 있음
    private final String[] description;


    StatType(String displayName, Material iconMaterial, int maxLevel, String guiTitle, String[] description) {
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.maxLevel = maxLevel;
        this.guiTitle = guiTitle;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Material getIconMaterial() { return iconMaterial; }
    public int getMaxLevel() { return maxLevel; }
    public String getGuiTitle() { return guiTitle; }
    public String[] getDescription() { return description; }
}