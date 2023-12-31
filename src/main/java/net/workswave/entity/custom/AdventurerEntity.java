package net.workswave.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.workswave.config.RottedConfig;
import net.workswave.entity.ai.CustomMeleeAttackGoal;
import net.workswave.entity.categories.RottedWeaponizedZombie;
import net.workswave.entity.categories.RottedZombie;
import net.workswave.registry.ItemRegistry;
import net.workswave.registry.SoundRegistry;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Objects;

public class AdventurerEntity extends RottedZombie implements GeoEntity, RottedWeaponizedZombie {


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public AdventurerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.xpReward = 10;
    }
    @Override
    protected void customServerAiStep() {
        if (!this.isNoAi() && GoalUtils.hasGroundPathNavigation(this)) {
            ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        }
        super.customServerAiStep();
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float a = (float) this.getAttributeValue(Attributes.ARMOR);
        float v = (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        f = f + EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) entity).getMobType()) / 2;
        if (entity instanceof AdventurerEntity) {
            a = a + EnchantmentHelper.getDamageProtection(this.getArmorSlots(), ((LivingEntity) entity).getLastDamageSource());
            v = v + EnchantmentHelper.getDamageProtection(this.getArmorSlots(), ((LivingEntity) entity).getLastDamageSource());
        }
        return super.doHurtTarget(entity);
    }
    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new LeapAtTargetGoal(this,0.2F));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7D, 25, true));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers(RottedZombie.class));
        this.goalSelector.addGoal(4, new CustomMeleeAttackGoal(this, 1.5, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return 1.5 + entity.getBbWidth() * entity.getBbWidth();
            }
        });
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true) {
            @Override
            public void start() {
                this.mob.swing(InteractionHand.MAIN_HAND);
                super.start();
            }
        });
    }

    @Nullable
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.ATTACK_KNOCKBACK, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 64D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.MAX_HEALTH, RottedConfig.SERVER.adventurer_health.get())
                .add(Attributes.ATTACK_DAMAGE, RottedConfig.SERVER.adventurer_damage.get())
                .add(Attributes.ARMOR, 6D);

    }



    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        RandomSource randomsource = pLevel.getRandom();
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        this.populateDefaultEquipmentEnchantments(randomsource, pDifficulty);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }




    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        ItemStack helmetG = ItemStack.EMPTY;
        ItemStack chestG = ItemStack.EMPTY;
        ItemStack mainG = ItemStack.EMPTY;

        for (String str : RottedConfig.DATAGEN.adventurer_helmet.get()) {
            String[] string = str.split("\\|" );
            ItemStack helmet = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(string[0]))));
            if (Math.random() < Integer.parseUnsignedInt(string[1]) / 100F) {
                helmetG = helmet;
            }
        }
        for (String str : RottedConfig.DATAGEN.adventurer_chestplate.get()) {
            String[] string = str.split("\\|" );
            ItemStack chest = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(string[0]))));
            if (Math.random() < Integer.parseUnsignedInt(string[1]) / 100F) {
                chestG = chest;
            }
        }
        for (String str : RottedConfig.DATAGEN.adventurer_main_hand.get()) {
            String[] string = str.split("\\|" );
            ItemStack main = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(string[0]))));
            if (Math.random() < Integer.parseUnsignedInt(string[1]) / 100F) {
                mainG = main;
            }
        }

        this.setItemSlot(EquipmentSlot.MAINHAND, mainG);
        this.setItemSlot(EquipmentSlot.HEAD, helmetG);
        this.setItemSlot(EquipmentSlot.CHEST, chestG);

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controller2) {
        controller2.add(
                new AnimationController<>(this, "controller", 7, event -> {
                    event.getController().setAnimationSpeed(0.5D);
                    if (event.isMoving()) {
                        event.getController().setAnimationSpeed(1D);
                        return event.setAndContinue(RawAnimation.begin().thenLoop("aggression"));
                    }
                    if(!event.isMoving()) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
                    }
                    return PlayState.CONTINUE;
                }));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public MobType getMobType() {
        return MobType.UNDEAD;
    }


    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.ENTITY_ROTTED_ZOMBIE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundRegistry.ENTITY_ROTTED_ZOMBIE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.ENTITY_ROTTED_ZOMBIE_DEATH.get();
    }


    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        super.playStepSound(pos, blockIn);
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.5F, 1.0F);
    }


    protected SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
        Entity entity = pSource.getEntity();
        if (Math.random() <= 0.1F) {
            this.spawnAtLocation(ItemRegistry.ROTTEN_BRAIN.get());
        }
        if (Math.random() <= 0.6F) {
            this.spawnAtLocation(Items.ROTTEN_FLESH);
        }
        if (Math.random() <= 0.1F) {
            this.spawnAtLocation(Items.ROTTEN_FLESH);
        }

    }
}
