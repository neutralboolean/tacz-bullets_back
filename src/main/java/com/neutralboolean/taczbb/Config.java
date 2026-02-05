package com.neutralboolean.taczbb;

import net.minecraft.util.Tuple;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.IntValue MIN_PISTOL_AMMO =
            BUILDER.comment("The minimum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("minAmmoPistol", 6, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_PISTOL_AMMO =
            BUILDER.comment("The maximum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoPistol", 20, 0, 256);

    private static final ForgeConfigSpec.IntValue MIN_RIFLE_AMMO =
            BUILDER.comment("The minimum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("minAmmoRifle", 8, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_RIFLE_AMMO =
            BUILDER.comment("The maximum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoRifle", 37, 0, 256);

    private static final ForgeConfigSpec.IntValue MIN_SHOTGUN_AMMO =
            BUILDER.comment("The minimum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("minAmmoShotgun", 3, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_SHOTGUN_AMMO =
            BUILDER.comment("The maximum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoShotgun", 14, 0, 256);

    private static final ForgeConfigSpec.IntValue MIN_SNIPER_AMMO =
            BUILDER.comment("The minimum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("minAmmoSniper", 5, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_SNIPER_AMMO =
            BUILDER.comment("The maximum number of bullets to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoSniper", 12, 0, 256);

    private static final ForgeConfigSpec.IntValue MIN_BARREL_AMMO =
            BUILDER.comment("The minimum number of rounds to return on a kill, for this type of gun")
                    .defineInRange("minAmmoBarrel", 1, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_BARREL_AMMO =
            BUILDER.comment("The maximum number of rounds to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoBarrel", 3, 0, 256);

    private static final ForgeConfigSpec.IntValue MIN_FUEL_AMMO =
            BUILDER.comment("The minimum number of fuel to return on a kill, for this type of gun")
                    .defineInRange("minAmmoFuel", 10, 0, 256);
    private static final ForgeConfigSpec.IntValue MAX_FUEL_AMMO =
            BUILDER.comment("The maximum number of fuel to return on a kill, for this type of gun")
                    .defineInRange("maxAmmoFuel", 20, 0, 256);

    private static final ForgeConfigSpec.DoubleValue AMMO_DROP_CHANCE =
            BUILDER.comment("The percentage chance that ammo should drop on a gun kill.")
                    .defineInRange("ammoDropChance", 0.25, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue SHOULD_DROP_AUX_AMMO =
            BUILDER.comment("Whether auxiliary ammo (ammo types other than the one used in the kill) should be dropped.")
                    .define("shouldDropAuxAmmo", true);
    private static final ForgeConfigSpec.IntValue AUX_AMMO_DROP_SPREAD =
            BUILDER.comment("How many of the other ammo types should be dropped in aux ammo drop.")
                    .defineInRange("auxAmmoDropRate", 2, 0, 5);
    private static final ForgeConfigSpec.DoubleValue AUX_AMMO_DROP_CHANCE =
            BUILDER.comment("The percentage chance that auxiliary ammo drops after normal ammo drops.")
                    .defineInRange("auxAmmoDropChance", 0.65, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue AUX_AMMO_DROP_RATE =
            BUILDER.comment("How much fewer aux ammo should drop than normal ammo. '1.0' means at equal rates to normal ammo.")
                    .defineInRange("auxAmmoDropChance", 0.85, 0.0, 1.0);
    private static final ForgeConfigSpec.BooleanValue SHOULD_BOSS_DROP_MORE =
            BUILDER.comment("Whether bosses (entities with the entity tag '#forge:bosses' or '#c:bosses') should drop more ammo.")
                    .define("shouldBossesDropMore", true);
    private static final ForgeConfigSpec.DoubleValue BOSS_AMMO_DROP_BUFF =
            BUILDER.comment("The percentage chance that auxiliary ammo drops after normal ammo drops.")
                    .defineInRange("bossAmmoBuff", 6.0, 1.0, 256.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Map<String, Tuple<Integer, Integer>> minMaxAmmoMap = new HashMap<>();

    public static double ammoDropChance;
    public static boolean shouldDropAuxAmmo;
    public static int auxAmmoSpread;
    public static double auxAmmoDropChance;
    public static double auxAmmoDropRate;

    public static boolean shouldBossesDropMore;
    public static double bossAmmoBuff;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        minMaxAmmoMap.putIfAbsent(Main.PISTOL_TYPE, syncSensibly(MIN_PISTOL_AMMO.get(), MAX_PISTOL_AMMO.get()));
        minMaxAmmoMap.putIfAbsent(Main.RIFLE_TYPE, syncSensibly(MIN_RIFLE_AMMO.get(), MAX_RIFLE_AMMO.get()));
        minMaxAmmoMap.putIfAbsent(Main.SHOTGUN_TYPE, syncSensibly(MIN_SHOTGUN_AMMO.get(), MAX_SHOTGUN_AMMO.get()));
        minMaxAmmoMap.putIfAbsent(Main.SNIPER_TYPE, syncSensibly(MIN_SNIPER_AMMO.get(), MAX_SNIPER_AMMO.get()));
        minMaxAmmoMap.putIfAbsent(Main.BARREL_TYPE, syncSensibly(MIN_BARREL_AMMO.get(), MAX_BARREL_AMMO.get()));
        minMaxAmmoMap.putIfAbsent(Main.FUEL_TYPE, syncSensibly(MIN_FUEL_AMMO.get(), MAX_FUEL_AMMO.get()));

        ammoDropChance = AMMO_DROP_CHANCE.get();
        shouldDropAuxAmmo = SHOULD_DROP_AUX_AMMO.get();
        auxAmmoSpread = AUX_AMMO_DROP_SPREAD.get();
        auxAmmoDropChance = AUX_AMMO_DROP_CHANCE.get();
        auxAmmoDropRate = AUX_AMMO_DROP_RATE.get();

        shouldBossesDropMore = SHOULD_BOSS_DROP_MORE.get();
        bossAmmoBuff = BOSS_AMMO_DROP_BUFF.get();
    }

    /**
     * A simple guard against misuse where the min and max values are reversed.
     * If `max` is less than `min`, just proceeds as if min and max are the same value.
     */
    private static Tuple<Integer, Integer> syncSensibly(Integer min, Integer max) {
        if (min > max) {
            return new Tuple<>(min, min);
        }
        return new Tuple<>(min, max);
    }
}
