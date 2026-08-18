package file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;

public class AssetManagerTest {

    @Test
    public void legacyWeaponDamageIsMovedToHitDamage() {
        Entity legacyWeapon = Entity.newBuilder()
                .setName("legacy axe")
                .setIsItem(true)
                .setDamage(Damage.newBuilder().setBruteDamage(3).setStructuralDamage(7))
                .setHitDamage(HitDamage.newBuilder().setHitCooldown(900))
                .build();

        Entity normalized = AssetManager.normalizeItemCombatStats(legacyWeapon);

        assertEquals(0, normalized.getDamage().getBruteDamage(), 0);
        assertEquals(0, normalized.getDamage().getStructuralDamage(), 0);
        assertEquals(3, normalized.getHitDamage().getBruteDamage(), 0);
        assertEquals(7, normalized.getHitDamage().getStructuralDamage(), 0);
        assertEquals(900, normalized.getHitDamage().getHitCooldown());
    }

    @Test
    public void existingHitDamageIsNotOverwritten() {
        Entity weapon = Entity.newBuilder()
                .setName("axe")
                .setIsItem(true)
                .setDamage(Damage.newBuilder().setStructuralDamage(99))
                .setHitDamage(HitDamage.newBuilder().setBruteDamage(5))
                .build();

        Entity normalized = AssetManager.normalizeItemCombatStats(weapon);

        assertEquals(0, normalized.getDamage().getStructuralDamage(), 0);
        assertEquals(5, normalized.getHitDamage().getBruteDamage(), 0);
        assertEquals(99, normalized.getHitDamage().getStructuralDamage(), 0);
        assertTrue(normalized.getIsItem());
    }
}
