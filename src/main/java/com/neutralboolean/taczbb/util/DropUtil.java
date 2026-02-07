package com.neutralboolean.taczbb.util;

import com.mojang.logging.LogUtils;
import com.neutralboolean.taczbb.Config;
import com.neutralboolean.taczbb.BBMain;
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
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.PISTOL_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.RIFLE_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.SHOTGUN_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.SNIPER_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.BARREL_TYPE));
        AMMOTYPE_RESOURCES.add(ResourceLocation.fromNamespaceAndPath(TudMod.MODID, BBMain.FUEL_TYPE));

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

    public List<ItemEntity> produceAmmoDrop(boolean doBossScaling,
                                            double numAssistingPlayers,
                                            final ItemStack weapon,
                                            final LivingEntity victim) {
        ResourceLocation ammoLoc = Objects.requireNonNull(AmmoManager.getAmmo(weapon),
                        "Successfully made IGun but couldn't retrieve ammo for Item that should be a gun.")
                .getA();

        double bossMult = 1.0;
        Double dropCount = 0.0;
        try {
            if (doBossScaling) {
                // bosses drop max ammo for weapon type before any scaling
                dropCount = Math.ceil(getAmmoDropCount(ammoLoc.getPath(), true)
                        * Config.bossAmmoBuff
                        * giveAssistAmmoScaling(numAssistingPlayers));
            } else {
                // dropCount between the min and max count for killing weapon
                dropCount = Math.ceil(getAmmoDropCount(ammoLoc.getPath()) * bossMult * giveAssistAmmoScaling(numAssistingPlayers));
            }
        } catch (NoSuchElementException nsee) {
            LOGGER.error("Failed to get ammo drop count...", nsee);
        }

        List<ItemEntity> ammoDrop = new ArrayList<>();
        if (dropCount != 0) {
            // should I retain the stacksize normalization for ammo drops?
            // yes: the maximum amount the config allows is 256, so it should make sure that it doesn't exceed stacksize.
            LOGGER.debug("Main ammo dropped: {} {} rounds", dropCount, ammoLoc.getPath());
            prepAmmoStack(AmmoManager.getGunAmmo(weapon), dropCount.intValue(), victim, ammoDrop);
        }

        return ammoDrop;
    }

    public Set<ItemEntity> produceAuxAmmoDrop(boolean doBossScaling,
                                              double numAssistingPlayers,
                                              int size,
                                              final ResourceLocation avoid,
                                              final LivingEntity victim) {
        var auxAmmoResources = giveAuxAmmoDrops(size, avoid);
        Set<ItemEntity> auxAmmoDrops = new HashSet<>();

        double bossMult = 1.0;
        if (doBossScaling) {
            bossMult = Config.bossAmmoBuff; // only done here because it'd keep getting assigned in the loop
        }

        for (ResourceLocation resourceLocation : auxAmmoResources) {
            int baseDropCount = 0;
            try {
                if (doBossScaling) {
                    // bosses drop max ammo for weapon type before any scaling
                    baseDropCount = getAmmoDropCount(resourceLocation.getPath(), true);
                } else {
                    baseDropCount = getAmmoDropCount(resourceLocation.getPath());
                }
            } catch (NoSuchElementException nsee) {
                LOGGER.error("Failed to get ammo drop count...", nsee);
            }

            var ratedDropCount = Math.round(baseDropCount * Config.auxAmmoDropRate);
            Double dropCount = ratedDropCount * bossMult * giveAssistAmmoScaling(numAssistingPlayers);
            LOGGER.debug("Aux ammo dropped: {} {} rounds", dropCount, resourceLocation.getPath());
            prepAmmoStack(AmmoItemBuilder.create().setId(resourceLocation).build(), dropCount.intValue(), victim, auxAmmoDrops);
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
            // blocks getting the same drop type of ammo more than once
            ammotypesCopy.remove(pick);
        }

        return auxAmmoSet;
    }

    public int getAmmoDropCount(String ammoType) throws NoSuchElementException {
        return getAmmoDropCount(ammoType, false);
    }

    public int getAmmoDropCount(String ammoType, boolean beMaxed) throws NoSuchElementException {
        var minMaxAmmoCountTuple = Config.minMaxAmmoMap.get(ammoType);
        if (minMaxAmmoCountTuple == null) {
            throw new NoSuchElementException("No such gun/ammo type.");
        }

        int amount = 0;
        if (beMaxed) {
            amount = minMaxAmmoCountTuple.getB();
        } else {
            int minAmmo = minMaxAmmoCountTuple.getA();
            int maxAmmo = minMaxAmmoCountTuple.getB();
            amount = generator.nextInt(minAmmo, maxAmmo+1);
        }

        return amount;
    }

    private double giveAssistAmmoScaling(double numAssistingPlayers) {
        var xVariable = 80.0 * Math.pow(numAssistingPlayers, 4.0);
        return Math.log10(xVariable + 10);
    }
}
