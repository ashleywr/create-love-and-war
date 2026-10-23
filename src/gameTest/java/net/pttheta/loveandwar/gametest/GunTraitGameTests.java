package net.pttheta.loveandwar.gametest;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.runtime.CraftedParts;
import net.pttheta.loveandwar.gun.runtime.GunTraits;
import net.pttheta.loveandwar.gun.runtime.GunTraitTooltips;
import net.pttheta.loveandwar.item.ModDataComponents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunTraitGameTests {
    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void declaredAlwaysOnAbilityIsNotAnUnconditionalStatSource(GameTestHelper helper) {
        var gun = net.pttheta.loveandwar.gun.content.GunDef.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("""
            {"actionGroups":[{"key":"primary_fire"}],"staticAbilities":[{
              "startTriggers":[{"triggerType":"AlwaysOn"}],"targets":[{"targetType":"Shooter"}],
              "effects":[{"effectType":"ApplyModifier","modifiers":[{
                "stat":"impact_damage","accumulators":[{"operation":"StackablePercentage","value":100}]
              }]}]
            }]}
            """)).getOrThrow();
        var sources = net.pttheta.loveandwar.gun.runtime.StatSources.forFiring(gun, gun.actionGroups().getFirst(),
                net.pttheta.loveandwar.gun.runtime.GunAttachments.EMPTY);
        helper.assertTrue(Math.abs(net.pttheta.loveandwar.gun.content.stat.StatResolver.resolve("impact_damage", 7, sources) - 7) < 0.0001,
                "AlwaysOn declaration must not alter damage when the reference never dispatches that trigger");
        helper.succeed();
    }

    private static ItemStack gun(String... parts) {
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:chassepot")));
        gun.set(ModDataComponents.CRAFTED_PARTS, new CraftedParts(java.util.Arrays.stream(parts)
                .map(name -> ResourceLocation.parse("createloveandwar:" + name)).toList()));
        return gun;
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void craftedGunTooltipRetainsSummedTraits(GameTestHelper helper) {
        var gun = gun("stock_wood", "handguard_wood", "barrel_iron");
        var lines = gun.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).toString();
        helper.assertTrue(lines.contains("ability.createloveandwar.stoic") && lines.contains("II"),
                "Two wooden parts must describe Stoic II; tooltip=" + lines);
        helper.assertTrue(lines.contains("ability.createloveandwar.precise"), "Iron barrel must describe Precise");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void equippedWoodenGunRestoresOriginalAttributeEffect(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "TraitTester"));
        var gun = gun("stock_wood", "handguard_wood");
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        double before = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - before - 1.0) < 0.0001,
                "Original nonstackable Stoic equip effect adds one toughness, even with trait level II");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - before - 1.0) < 0.0001,
                "An equipped ability must not accumulate once per tick");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - before - 1.0) < 0.0001,
                "Original quirk: endTriggers are not evaluated and ApplyAttribute.EndServer does not remove the transient modifier");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void traitsCapLevelsAndSurviveItemReload(GameTestHelper helper) {
        var gun = gun("stock_wood", "handguard_wood", "grip_wood", "stock_wood", "handguard_wood", "grip_wood",
                "barrel_iron", "barrel_iron", "barrel_iron", "barrel_iron", "barrel_iron", "barrel_iron", "absent_part");
        var traits = GunTraits.resolve(gun);
        helper.assertTrue(traits.size() == 2, "Unknown part must not introduce an invalid trait");
        helper.assertTrue(traits.get(ResourceLocation.parse("createloveandwar:stoic")).level() == 5, "Stoic must cap at level V");
        helper.assertTrue(traits.get(ResourceLocation.parse("createloveandwar:precise")).level() == 5, "Precise must cap at level V");
        var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (net.minecraft.nbt.CompoundTag)gun.save(helper.getLevel().registryAccess()));
        helper.assertTrue(GunTraits.resolve(restored).equals(traits), "Trait levels must be derived identically after item reload");
        helper.assertTrue(GunTraitTooltips.lines(restored, true).toString().contains("tooltip.createloveandwar.crafted_from")
                        && GunTraitTooltips.lines(restored, true).toString().contains("ability.createloveandwar.stoic.tooltip.5"),
                "Expanded details must include the materials and the capped trait description");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void offhandTraitsDoNotDuplicateSharedAttributeModifiers(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "OffhandTraitTester"));
        double before = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        player.setItemInHand(InteractionHand.OFF_HAND, gun("stock_wood"));
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - before - 1) < 0.0001,
                "Offhand gun must dispatch Equip");
        player.setItemInHand(InteractionHand.MAIN_HAND, gun("stock_wood", "handguard_wood"));
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - before - 1) < 0.0001,
                "Original shared effect identity replaces, rather than duplicates, the modifier across guns");
        helper.succeed();
    }
}
