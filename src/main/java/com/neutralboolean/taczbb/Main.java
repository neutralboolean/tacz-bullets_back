package com.neutralboolean.taczbb;

import com.mojang.logging.LogUtils;
import com.neutralboolean.taczbb.util.DropUtil;
import com.scarasol.tud.manager.AmmoManager;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Main.MODID)
public class Main {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "taczbb";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static String PISTOL_TYPE = "pistol";
    public static String RIFLE_TYPE = "rifle";
    public static String SHOTGUN_TYPE = "shot";
    public static String SNIPER_TYPE = "sniper";
    public static String BARREL_TYPE = "barrel";
    public static String FUEL_TYPE = "fuel_tank";

    private DropUtil dropUtil;

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
//        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

//    private void commonSetup(final FMLCommonSetupEvent event) {
//        // Some common setup code
//        LOGGER.info("HELLO FROM COMMON SETUP");
//    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.debug("Grabbing DropUtil instance.");
        this.dropUtil = DropUtil.getInstance();
    }

    @Deprecated
    private Optional<CommonGunIndex> getGunIndex(ItemStack itemStack) {
        var iGun = IGun.getIGunOrNull(itemStack);
        if  (iGun == null) {
            return Optional.empty();
        }
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(itemStack));
    }

    @SubscribeEvent
    public void drops(LivingDropsEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof LivingEntity user)) {
                return;
            }

            var weapon = user.getMainHandItem();
            var IGUN = IGun.getIGunOrNull(weapon);
            if (IGUN != null) {
                var victim = event.getEntity();
//                var gunId = IGUN.getGunId(weapon);

                // dropRate is variable, from config
                if (!dropUtil.doCheck(Config.ammoDropChance)) {
                    return; //skip the rest
                }

                var droppedAmmoSet = dropUtil.produceAmmoDrop(weapon, victim);
                for (ItemEntity droppedAmmo : droppedAmmoSet) {
                    event.getDrops().add(droppedAmmo);
                }

                // add auxiliary drops
                // randomly decide on which ammo types are dropped
                if (!dropUtil.doCheck(Config.auxAmmoDropChance)) {
                    return;
                }

                var auxDroppedAmmoSet =
                        dropUtil.produceAuxAmmoDrop(
                                Config.auxAmmoSpread,
                                Objects.requireNonNull(AmmoManager.getAmmo(weapon))
                                        .getA(),
                                victim);
                event.getDrops().addAll(auxDroppedAmmoSet);
            }

        } catch (NoSuchElementException nsee) {
            LOGGER.error("Failure in getting ammo count.",nsee);
        } catch (NullPointerException npe) {
            LOGGER.warn("Failure in retrieving ammo type from hand item.", npe);
        } catch (ArithmeticException ae) {
            LOGGER.error("scaledRandom might have overflowed int space.", ae);
        }
    }


    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.debug("No necessary client setup.");
        }
    }
}
