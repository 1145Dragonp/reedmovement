
package BreedMovement;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Collections;

/**
 * Handler for animal love mode and breeding events.
 * Triggers "kissing" when two animals get close enough while both are in love mode.
 * Delegates physics actions to BreedMovementAction.
 */
@EventBusSubscriber(modid = MainClass.MODID)
public class LoveModeEventHandler {

    //@AI(BAPI.Animal,spawnChildFromBreeding)
    // 原版 BreedGoal 的繁殖条件: loveTime >= 60 tick 且 distanceToSqr(partner) < 9.0 (即实际距离 < 3.0 格)
    // 所以这里必须 >= 3.0，才能保证在原版生下幼崽之前先触发 kiss 事件
    // 之前的 1.0 太小，动物还没走近 1 格内，原版就在 3 格内直接繁殖了
    // Maximum distance for kiss to trigger (must be >= vanilla breeding distance 3.0)
    private static final double MAX_LOVE_DISTANCE = 2.0;

    // Track which animals have already triggered the kissing event
    // WeakHashMap allows GC to collect entries when animals die
    private static final Set<Animal> kissingAnimals = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Logger log = LoggerFactory.getLogger(LoveModeEventHandler.class);

    /**
     * Listen to entity tick to check love mode and distance.
     * Triggers the "kissing" event when two animals first get close enough.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Animal animal)) return;
        if (animal.level().isClientSide) return; // Server-side only

        // Check if the animal is in love mode
        if (!animal.isInLove()) return;

        // Search for nearby lovers within expanded range (4 blocks to catch approaching animals)
        AABB searchBox = animal.getBoundingBox().inflate(4.0);
        List<Animal> allLovers = animal.level().getEntitiesOfClass(
                Animal.class,
                searchBox,
                e -> e != animal && e.isInLove() && e.getType() == animal.getType()
        );

        boolean hasPartnerNearby = false;
        for (Animal lover : allLovers) {
            if (animal.distanceTo(lover) <= MAX_LOVE_DISTANCE) {
                hasPartnerNearby = true;
                break;
            }
        }

        if (hasPartnerNearby) {
            // Animal has a partner nearby - trigger kissing if not already triggered
            if (!kissingAnimals.contains(animal)) {
                kissingAnimals.add(animal);
                onKissTriggered(animal);
            }
        } else {
            // No partner nearby - cancel love mode and reset kissing state
            kissingAnimals.remove(animal);
            animal.setAge(0);
        }
    }

    /**
     * Called when two animals first get close enough to start kissing.
     * Delegates the physics action to BreedMovementAction.
     */
    private static void onKissTriggered(Animal animal) {
        // Find the partner
        AABB searchBox = animal.getBoundingBox().inflate(MAX_LOVE_DISTANCE);
        List<Animal> allLovers = animal.level().getEntitiesOfClass(
                Animal.class,
                searchBox,
                e -> e != animal && e.isInLove() && e.getType() == animal.getType()
        );

        if (!allLovers.isEmpty()) {
            Animal partner = allLovers.get(0);
            log.info("[BreedMovement] KISS triggered! "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType())
                    + " at " + animal.blockPosition()
                    + " with partner "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(partner.getType())
                    + " at " + partner.blockPosition());
            /*
            System.out.println("[BreedMovement] KISS triggered! "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType())
                    + " at " + animal.blockPosition()
                    + " with partner "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(partner.getType())
                    + " at " + partner.blockPosition());

             */

            // ===== 委托给 BreedMovementAction 处理物理效果 =====
            BreedMovementAction.applyThrust(animal, partner);
            // 也给伴侣施加效果（让两只动物都有前后移动）
            BreedMovementAction.applyThrust(partner, animal);
        }
    }

    /**
     * Listen to baby spawn event.
     */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getChild().level().isClientSide) return;

        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();

        if (!(parentA instanceof Animal mother) || !(parentB instanceof Animal father)) {
            return;
        }

        System.out.println("[BreedMovement] Baby spawned! Mother: "
                + BuiltInRegistries.ENTITY_TYPE.getKey(mother.getType())
                + ", Father: "
                + BuiltInRegistries.ENTITY_TYPE.getKey(father.getType()));
    }
}