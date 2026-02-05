package com.neutralboolean.taczbb.util;

import com.mojang.logging.LogUtils;
import com.neutralboolean.taczbb.Config;
import com.neutralboolean.taczbb.Main;
import com.scarasol.tud.TudMod;
import com.scarasol.tud.manager.AmmoManager;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import org.slf4j.Logger;

import java.util.*;

public class DropUtil {
    private static DropUtil instance = null;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Random generator;
    private final List<ResourceLocation> AMMOTYPE_RESOURCES;

    private DropUtil () {
        AMMOTYPE_RESOURCES = new ArrayList<>();
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.PISTOL_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.RIFLE_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.SHOTGUN_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.SNIPER_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.BARREL_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, Main.FUEL_TYPE));

        this.generator = new Random();
    }

    public static DropUtil getInstance() {
        if (instance == null) {
            instance = new DropUtil();
        }

        return instance;
    }

    /**
     * Simple encapsulation of a random check.
     * Like a d100 percentile die, this checks that random double rolls under `percentage`.
     * @param percentage of success of random check, e.g. between 0.0 and 1.0
     * @return true if should do check based on `percentage`, else false
     */
    public boolean doCheck(double percentage) {
        return generator.nextDouble() <= percentage;
    }

    public List<ItemEntity> produceAmmoDrop(final ItemStack weapon, final LivingEntity victim) {
        ResourceLocation ammoLoc = Objects.requireNonNull(AmmoManager.getAmmo(weapon),
                        "Successfully made IGun but couldn't retrieve ammo for Item that should be a gun.")
                .getA();

        double bossMult = 1.0;
        if (Config.shouldBossesDropMore) {
            if (victim.getType().is(Tags.EntityTypes.BOSSES)) {
                bossMult = Config.bossAmmoBuff;
            }
        }
        List<ItemEntity> ammoDrop = new ArrayList<>();

        // dropCount between the min and max count for killing weapon
        int dropCount = Double.valueOf(getAmmoDropCount(ammoLoc.getPath()) * bossMult).intValue();

        if (dropCount != 0) {
            // should I retain the stacksize normalization for ammo drops?
            // yes: the maximum amount the config allows is 256, so it should make sure that it doesn't exceed stacksize.
            prepAmmoStack(AmmoManager.getGunAmmo(weapon), dropCount, victim, ammoDrop);
        }

        return ammoDrop;
    }

    public Set<ItemEntity> produceAuxAmmoDrop(int size, final ResourceLocation avoid, final LivingEntity victim) {
        var auxAmmoResources = giveAuxAmmoDrops(size, avoid);
        Set<ItemEntity> auxAmmoDrops = new HashSet<>();

        double bossMult = 1.0;
        if (Config.shouldBossesDropMore) {
            if (victim.getType().is(Tags.EntityTypes.BOSSES)) {
                bossMult = Config.bossAmmoBuff;
            }
        }

        for (ResourceLocation resourceLocation : auxAmmoResources) {
            var rawDropCount = getAmmoDropCount(resourceLocation.getPath());
            var ratedDropCount = Math.round(rawDropCount * Config.auxAmmoDropRate);
            int dropCount = Double.valueOf(Long.valueOf(ratedDropCount).doubleValue() * bossMult).intValue();
            LOGGER.debug("Aux ammo dropped: {} {} rounds", dropCount, resourceLocation.getPath());
            prepAmmoStack(AmmoItemBuilder.create().setId(resourceLocation).build(), dropCount, victim, auxAmmoDrops);
        }

        return auxAmmoDrops;
    }

    private void prepAmmoStack(ItemStack ammoStack, int dropCount, final LivingEntity victim, Collection<ItemEntity> ammoCollection) {
        // processes drops over loop until the drop size is less than a stack
        while (dropCount > 0) {
            var dropped = Math.min(ammoStack.getMaxStackSize(), dropCount);
            ammoStack.setCount(dropped);
            ammoCollection.add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), ammoStack));

            dropCount -= dropped;
        }
    }

    /**
     *
     * @param size the number of draws to pull from the set of ammo types
     * @return a Set containing the ResourceLocation of ammo types with length equal to `num`
     */
    public Set<ResourceLocation> giveAuxAmmoDrops(int size) {
        return giveAuxAmmoDrops(size, ResourceLocation.withDefaultNamespace("air"));
    }

    /**
     *
     * @param size the number of draws to pull from the set of ammo types
     * @param avoid type of ammo to avoid drawing
     * @return a Set containing the ResourceLocation of ammo types with length equal to `num`
     */
    public Set<ResourceLocation> giveAuxAmmoDrops(int size, final ResourceLocation avoid) {
        var ammotypesCopy = new ArrayList<>(AMMOTYPE_RESOURCES);
        ammotypesCopy.remove(avoid);

        Set<ResourceLocation> auxAmmoSet = new HashSet<>();
        //avoids weird edge case where config allowed improper number of aux ammo drops
        for (int i = 0; i < size && i < AMMOTYPE_RESOURCES.size(); i++) {
            var pick = ammotypesCopy.get(generator.nextInt(0, ammotypesCopy.size()));
            auxAmmoSet.add(pick);
            // blocks getting the same drop of ammo more than once
            ammotypesCopy.remove(pick);
        }

        return auxAmmoSet;
    }

    public int getAmmoDropCount(String ammoType) throws ArithmeticException, NoSuchElementException {
        var minMaxAmmoCountTuple = Config.minMaxAmmoMap.get(ammoType);
        if (minMaxAmmoCountTuple == null) {
            throw new NoSuchElementException("No such gun/ammo type.");
        }

        int minAmmo = minMaxAmmoCountTuple.getA();
        int maxAmmo = minMaxAmmoCountTuple.getB();
        return generator.nextInt(minAmmo, maxAmmo+1);
    }
}
