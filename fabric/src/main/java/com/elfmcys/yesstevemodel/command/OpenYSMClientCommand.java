package com.elfmcys.yesstevemodel.command;

import com.elfmcys.yesstevemodel.command.subcommands.client.CacheCommand;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;


public class OpenYSMClientCommand {

    public static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> commandDispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = LiteralArgumentBuilder.<FabricClientCommandSource>literal("openysm")
                .requires(source -> true);

        root.then(CacheCommand.register());

        commandDispatcher.register(root);
    }
}
