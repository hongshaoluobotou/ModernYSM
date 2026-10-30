package rip.ysm.api.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * 命令注册的加载器无关入口（对应 Fabric 的 {@code CommandRegistrationCallback}）。
 *
 * <p>回调签名保持 Brigadier 原始形态（dispatcher, buildContext, selection），
 * 各加载器在自己的注册钩子里把原生参数原样转进来。
 *
 * <p>客户端侧命令未纳入本抽象：Fabric 的客户端命令源类型是 {@code FabricClientCommandSource}，
 * 与 NeoForge/Forge 的 {@code CommandSourceStack} 不兼容，由各加载器模块自行注册。
 */
public final class CommandEvents {

    private CommandEvents() {
    }

    @FunctionalInterface
    public interface CommandRegistrar {
        void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext,
                      Commands.CommandSelection selection);
    }

    /** 注册服务端命令（Fabric：{@code CommandRegistrationCallback}）。 */
    public static void onRegisterCommands(CommandRegistrar registrar) {
        rip.ysm.api.impl.EventBusImpl.onRegisterCommands(registrar);
    }
}
