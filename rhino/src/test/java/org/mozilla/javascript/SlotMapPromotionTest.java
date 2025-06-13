package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Ensures that slot map promotion */
class SlotMapPromotionTest {
    @Test
    public void promotionFromEmptyToSingleSlot() {
        assertPromotes(() -> SlotMapOwner.EMPTY_SLOT_MAP, SlotMapOwner.SingleEntrySlotMap.class);
    }

    @Test
    public void promotionFromSingleToEmbedded() {
        assertPromotes(
                () -> new SlotMapOwner.SingleEntrySlotMap(new Slot(new Object(), 0, 0)),
                EmbeddedSlotMap.class);
    }

    @Test
    public void promotionFromEmbeddedToHash() {
        assertPromotes(
                () -> {
                    var map = new EmbeddedSlotMap(SlotMapOwner.LARGE_HASH_SIZE);
                    fillToCapacity(SlotMapOwner.LARGE_HASH_SIZE, map);
                    return map;
                },
                HashSlotMap.class);
    }

    @Test
    public void promotionFromThreadSafeEmptyToSingleSlot() {
        assertPromotes(() -> SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP, SlotMapOwner.ThreadSafeSingleEntrySlotMap.class);
    }

    @Test
    public void promotionFromThreadSafeSingleToEmbedded() {
        assertPromotes(
                () -> new SlotMapOwner.ThreadSafeSingleEntrySlotMap(new Slot(new Object(), 0, 0)),
                ThreadSafeEmbeddedSlotMap.class);
    }

    @Test
    public void promotionFromThreadSafeEmbeddedToHash() {
        assertPromotes(
                () -> {
                    var map = new ThreadSafeEmbeddedSlotMap(SlotMapOwner.LARGE_HASH_SIZE);
                    fillToCapacity(SlotMapOwner.LARGE_HASH_SIZE, map);
                    return map;
                },
                ThreadSafeHashSlotMap.class);
    }

    private static void fillToCapacity(int size, EmbeddedSlotMap map) {
        // The EmbeddedSlotMap will promote once it's 3/4s full. It also
        // is always sized to a power of two.
        int b = Integer.highestOneBit(size);
        int nextPowerOfTwo = b << (size > b ? 1 : 0);
        for (int i = 0; i < nextPowerOfTwo / 4 * 3; ++i) {
            map.add(null, new Slot(Integer.toString(i), i, 0));
        }
    }

    private void assertPromotes(
            Supplier<SlotMap> slotMapSupplier, Class<? extends SlotMap> expectedClass) {
        ScriptableObject obj = new TestScriptableObject();
        obj.setMap(slotMapSupplier.get());

        // add one more slot to ensure the promotion
        obj.put("xxx", obj, "one more property");

        assertEquals(expectedClass, obj.getMap().getClass());
    }

    private static class TestScriptableObject extends ScriptableObject {
        public String getClassName() {
            return "foo";
        }
    }
}
