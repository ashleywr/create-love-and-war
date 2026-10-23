package net.pttheta.loveandwar.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.StatSources;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class AccumulationInputGameTests {
    private static List<ModifierSource> sources(int count, Modifier modifier) {
        var gun = GunContent.gun("g11k2").orElseThrow();
        var group = net.pttheta.loveandwar.gun.content.ActionGroup.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"key\":\"primary_fire\"}")).getOrThrow();
        // Original GetAttachmentStacks counts nonempty stacks once, not item counts or only recognized definitions.
        var attachments = count == 0 ? GunAttachments.EMPTY : GunAttachments.EMPTY.with("generic",
                List.of(new ItemStack(Items.APPLE, 64), ItemStack.EMPTY, new ItemStack(Items.STICK)));
        var sources = new ArrayList<>(StatSources.forFiring(gun, group, attachments));
        sources.add(ModifierSource.of("test", List.of(modifier)));
        return sources;
    }

    private static Modifier modifier(String stat, int value, String kind) {
        return Modifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"stat\":\"" + stat + "\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":" + value + ",\"multiplyPer\":[\"" + kind + "\"]}]}" )).getOrThrow();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void attachmentInputsCountSlotsAndSurviveProjectileSave(GameTestHelper helper) {
        var owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "InputTest"));
        owner.setPos(helper.absoluteVec(new Vec3(2, 6, 2)));
        var authored = BulletDef.ProjectileDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"acceleration":0,"impacts":[{"targetType":"ShotEntity","impactEffects":[{"effectType":"ApplyDamage","modifiers":[
                {"stat":"impact_damage","accumulators":[{"operation":"BaseAdd","value":2,"multiplyPer":["PerAttachment"]}]}]}]}]}
                """)).getOrThrow();
        for (int count : new int[]{0, 2}) {
            var firing = sources(count, modifier("acceleration", 2, "PerAttachment"));
            firing.add(ModifierSource.of("damage", List.of(modifier("impact_damage", 3, "PerAttachment"))));
            var bullet = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
            var loaded = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
            var pig = helper.spawn(EntityType.PIG, new Vec3(4, 6, 4));
            pig.setNoAi(true);
            try {
                bullet.initialize(owner, ResourceLocation.parse("createloveandwar:gyrojet_basic"), authored, firing, "primary_fire", null, 0);
                helper.assertTrue(bullet.definition().motion().acceleration() == 2 * count, "Acceleration must use actual attachment count " + count);
                var saved = new CompoundTag(); bullet.saveWithoutId(saved); loaded.load(saved); loaded.setOwner(owner);
                float health = pig.getHealth();
                loaded.detonate(new EntityHitResult(pig, pig.position().add(0, 0.01, 0)));
                helper.assertTrue(Math.abs(pig.getHealth() - (health - 5 * count)) < 0.00001,
                        "Saved firing and effect-local modifiers must both retain attachment count " + count + "; health=" + pig.getHealth());
            } finally { bullet.discard(); loaded.discard(); pig.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void repeatedInputFlagsAreAppliedOnceLikeOriginalFormula(GameTestHelper helper) {
        var repeated = Modifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"stat":"test","accumulators":[{"operation":"BaseAdd","value":3,"multiplyPer":["PerAttachment","PerAttachment"]}]}
                """)).getOrThrow();
        helper.assertTrue(StatResolver.resolve("test", 0, sources(2, repeated)) == 6, "Original formula stores input flags once, even when repeated");
        helper.succeed();
    }
}
