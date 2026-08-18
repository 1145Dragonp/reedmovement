package BreedMovement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreedMovementAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreedMovementAction.class);

    // 推力力度 (0.5 ~ 1.0 之间效果较好，可根据需求调整)
    private static final double STRENGTH = 0.3;

    /**
     * 【兼容接口】
     * 专门为 LoveModeEventHandler 提供的方法。
     * 繁殖瞬间调用，给 animal 一个远离 partner 的瞬时速度。
     */
    public static void applyThrust(LivingEntity animal, LivingEntity partner) {
        if (animal == null || partner == null || animal.level().isClientSide) {
            return;
        }

        // 1. 计算从 partner 指向 animal 的方向向量
        Vec3 partnerPos = partner.position();
        Vec3 animalPos = animal.position();
        Vec3 direction = animalPos.subtract(partnerPos).normalize();

        // 2. 如果距离太近导致方向为0，则随机给一个方向防止报错
        if (direction.lengthSqr() == 0) {
            direction = new Vec3(1, 0, 0);
        }

        // 3. 施加瞬时速度 (保留原有的 Y 轴运动，如跳跃或跌落)
        Vec3 currentMotion = animal.getDeltaMovement();
        Vec3 newMotion = new Vec3(
                direction.x * STRENGTH,
                currentMotion.y, // 保持垂直方向的原有力
                direction.z * STRENGTH
        );

        animal.setDeltaMovement(newMotion);

        // 4. 标记实体已修改过速度，防止被碰撞箱或摩擦力瞬间抵消
        animal.hurtMarked = true;
        /*
        LOGGER.info("[BreedMovement] 实体 {} 受到繁殖推力，方向: {}",
                animal.getType().builtInRegistryHolder().key().location(),
                direction);

         */
        if (animal.getRandom().nextDouble() < 0.1) {
            ItemEntity waterBucket = new ItemEntity(
                    animal.level(),
                    animal.getX(),
                    animal.getY() + 0.5,
                    animal.getZ(),
                    new ItemStack(Items.WATER_BUCKET)
            );
            // 给物品一个向上的微小速度，模拟掉落效果
            waterBucket.setDeltaMovement(0, 0.1, 0);
            animal.level().addFreshEntity(waterBucket);
        }

        if (animal.getRandom().nextDouble() < 0.1) {
            ItemEntity waterBucket = new ItemEntity(
                    animal.level(),
                    animal.getX(),
                    animal.getY() + 0.5,
                    animal.getZ(),
                    new ItemStack(Items.MILK_BUCKET)
            );
            // 给物品一个向上的微小速度，模拟掉落效果
            waterBucket.setDeltaMovement(0, 0.1, 0);
            animal.level().addFreshEntity(waterBucket);
        }

        if (animal.getRandom().nextDouble() < 0.005) {

            // 1. 在动物当前位置生成末地烛掉落物
            ItemEntity endRod = new ItemEntity(
                    animal.level(),
                    animal.getX(),
                    animal.getY() + 0.5,
                    animal.getZ(),
                    new ItemStack(Items.END_ROD)
            );
            // 给物品一个向上的微小速度，模拟掉落效果
            endRod.setDeltaMovement(0, 0.1, 0);
            animal.level().addFreshEntity(endRod);
            // 2. 强制实体暴毙 (无视护甲和抗性)
            animal.kill();

        }

    }

}