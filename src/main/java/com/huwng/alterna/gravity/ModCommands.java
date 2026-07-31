package com.huwng.alterna.gravity;

import com.huwng.alterna.Alterna;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * Đăng ký lệnh /gravitydebug để test lưu trữ dữ liệu trọng lực ở Bước 1.
 */
@EventBusSubscriber(modid = Alterna.MODID)
public class ModCommands {

    private static final SuggestionProvider<CommandSourceStack> DIRECTION_SUGGESTIONS =
            (context, builder) -> {
                Arrays.stream(Direction.values())
                        .map(d -> d.getSerializedName())
                        .forEach(builder::suggest);
                return builder.buildFuture();
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("gravitydebug")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("set")
                                .then(Commands.argument("direction", StringArgumentType.word())
                                        .suggests(DIRECTION_SUGGESTIONS)
                                        .executes(ModCommands::executeSet)))
                        .then(Commands.literal("get")
                                .executes(ModCommands::executeGet))
        );
    }

    private static int executeSet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String raw = StringArgumentType.getString(ctx, "direction");

        Direction direction = Arrays.stream(Direction.values())
                .filter(d -> d.getSerializedName().equalsIgnoreCase(raw))
                .findFirst()
                .orElse(null);

        if (direction == null) {
            ctx.getSource().sendFailure(Component.literal("Hướng không hợp lệ: " + raw));
            return 0;
        }

        GravityApi.setDirection(player, direction);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Đã set gravity direction = " + direction.getSerializedName()
                        + " cho " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static int executeGet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Direction current = GravityApi.getDirection(player);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Gravity direction hiện tại: " + current.getSerializedName()),
                false
        );
        return 1;
    }
}
