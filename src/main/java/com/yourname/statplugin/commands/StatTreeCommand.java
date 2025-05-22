package com.yourname.statplugin.commands;

import com.yourname.statplugin.gui.StatGUI;
import com.yourname.statplugin.stats.PlayerStatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatTreeCommand implements CommandExecutor {

    private final PlayerStatManager playerStatManager;

    public StatTreeCommand(PlayerStatManager playerStatManager) {
        this.playerStatManager = playerStatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        StatGUI.openMainStatGUI(player, playerStatManager);
        return true;
    }
}