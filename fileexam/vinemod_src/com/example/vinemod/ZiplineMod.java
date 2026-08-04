package com.example.vinemod;

import com.example.vinemod.client.ClientConfigSetup;
import com.example.vinemod.client.ZiplineClient;
import com.example.vinemod.config.ModConfig;
import com.example.vinemod.network.ConfigSyncPayload;
import com.example.vinemod.platform.NeoForgeRegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod("vinemod")
public class VineMod {
   public VineMod(IEventBus modEventBus, ModContainer modContainer) {
      NeoForgeRegistryHelper.ITEMS.register(modEventBus);
      NeoForgeRegistryHelper.SOUNDS.register(modEventBus);
      VineMod.init();
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener(this::clientSetup);
      modEventBus.addListener(this::registerPayloads);
      NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
      if (FMLEnvironment.getDist().isClient()) {
         ClientConfigSetup.register(modContainer);
         NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
      }

   }

   private void commonSetup(FMLCommonSetupEvent event) {
   }

   private void clientSetup(FMLClientSetupEvent event) {
      ZiplineClient.init();
   }

   private void registerPayloads(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1.0.0");
      registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!Minecraft.getInstance().hasSingleplayerServer()) {
               ModConfig.setServerConfig(payload.toModConfig());
            }

         }));
   }

   private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
      Player var3 = event.getEntity();
      if (var3 instanceof ServerPlayer serverPlayer) {
         PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromModConfig(ModConfig.get()), new CustomPacketPayload[0]);
      }

   }

   private void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
      ModConfig.restoreLocalConfig();
   }
}
