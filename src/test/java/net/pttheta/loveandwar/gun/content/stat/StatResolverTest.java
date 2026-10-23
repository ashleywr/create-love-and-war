package net.pttheta.loveandwar.gun.content.stat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatResolverTest {
    @Test void bundledContentSelfCheckUsesOfficialStatSemantics() {
        net.pttheta.loveandwar.gun.content.ContentSelfCheck.main(new String[0]);
    }

    private static float resolve(float base, Accumulator... accumulators) {
        return StatResolver.resolve("recoil", base, List.of(ModifierSource.of("test",
                List.of(new Modifier("recoil", List.of(), List.of(accumulators), "")))));
    }

    private static Accumulator independent(float value) {
        return new Accumulator(AccumulatorOp.INDEPENDENT_PERCENTAGE, value, List.of());
    }

    @Test void independentPercentageIsPercentageOfBaseRatherThanAnIncrease() {
        assertEquals(5, resolve(10, independent(50)), 0.0001);
        assertEquals(0.8, resolve(10, independent(8)), 0.0001);
        assertEquals(2.5, resolve(10, independent(50), independent(50)), 0.0001);
    }

    @Test void additionsStackablePercentagesAndIndependentPercentagesKeepOfficialOrder() {
        assertEquals(15, resolve(10,
                new Accumulator(AccumulatorOp.BASE_ADD, 10, List.of()),
                new Accumulator(AccumulatorOp.STACKABLE_PERCENTAGE, 50, List.of()),
                independent(50)), 0.0001);
    }

    @Test void legacyMultipliersKeepTheirMeaningAfterDecoding() {
        var modifier = Modifier.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"Stat\":\"recoil\",\"Multiply\":0.5}")).getOrThrow();
        assertEquals(5, StatResolver.resolve("recoil", 10,
                List.of(ModifierSource.of("legacy", List.of(modifier)))), 0.0001);
    }

    @Test void originalActiveCachesDoNotApplyDeclaredGroupPathFilters() {
        var declared = new Modifier("impact_damage", List.of("other_group"),
                List.of(new Accumulator(AccumulatorOp.BASE_ADD, 3, List.of())), "");
        var sources = List.of(ModifierSource.of("attachment", List.of(declared)));
        assertEquals(3, StatResolver.resolve("impact_damage", 0, sources, ModifierContext.ofGroups(List.of("primary_fire"))));
        var override = new Modifier("response_to_block", List.of("other_group"), List.of(), "Stick");
        assertEquals("Stick", StatResolver.resolveOverride("response_to_block", "Bounce", List.of(ModifierSource.of("attachment", List.of(override))),
                ModifierContext.ofGroups(List.of("primary_fire"))));
    }
}
