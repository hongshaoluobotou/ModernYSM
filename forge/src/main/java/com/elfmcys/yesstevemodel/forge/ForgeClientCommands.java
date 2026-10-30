package com.elfmcys.yesstevemodel.forge;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Forge 侧的客户端命令（{@code /openysm cache dump}）。
 *
 * <p>Forge 的 {@code RegisterClientCommandsEvent} 给的是 {@code CommandSourceStack}
 * （与 fabric 的 {@code FabricClientCommandSource} 不兼容），故命令体在加载器模块内实现。
 */
public final class ForgeClientCommands {

    private ForgeClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.<CommandSourceStack>literal("openysm")
                .requires(source -> true)
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("cache")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("dump")
                                .executes(ForgeClientCommands::dumpCache)));
        dispatcher.register(root);
    }

    private static int dumpCache(CommandContext<CommandSourceStack> context) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        player.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal("开始解析并导出客户端缓存模型...")));

        ClientModelManager.exportAllCachedModels(null, exportResult -> {
            if (exportResult.getMessage() != null) {
                player.sendSystemMessage(YSMMessageFormatter.withPrefix(exportResult.getMessage()));
            }
            if (exportResult.isSuccess()) {
                player.sendSystemMessage(Component.translatable("commands.yes_steve_model.export.success", exportResult.getFilePath()));
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
