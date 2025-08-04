package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SlotMapConcurrentLocksPromotionTest {
    @Test
    public void singleThreadPromotionInCompute_emptyToOne() {
        ScriptableObject obj = new TestScriptableObject();
        obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

        obj.getMap()
                .compute(
                        obj,
                        "foo",
                        0,
                        (key, index, existing, mutableMap, owner) -> {
                            assertSame(owner, obj);

                            mutableMap.add(owner, new Slot("a", 1, 0));

                            return null;
                        });

        assertArrayEquals(new Object[] {"a"}, obj.getIds());
    }

    @Test
    public void singleThreadPromotionInCompute_emptyToTwo() {
        ScriptableObject obj = new TestScriptableObject();
        obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

        obj.getMap()
                .compute(
                        obj,
                        "foo",
                        0,
                        (key, index, existing, mutableMap, owner) -> {
                            assertSame(owner, obj);

                            mutableMap.add(owner, new Slot("a", 1, 0));
                            mutableMap.add(owner, new Slot("b", 2, 0));

                            return null;
                        });

        assertArrayEquals(new Object[] {"a", "b"}, obj.getIds());
    }

    @Test
    public void singleThreadPromotionInCompute_oneToTwo() {
        ScriptableObject obj = new TestScriptableObject();
        obj.setMap(new SlotMapOwner.SingleEntrySlotMap(new Slot("a", 1, 0)));

        obj.getMap()
                .compute(
                        obj,
                        "foo",
                        0,
                        (key, index, existing, mutableMap, owner) -> {
                            assertSame(owner, obj);

                            mutableMap.add(owner, new Slot("b", 2, 0));

                            return null;
                        });

        assertArrayEquals(new Object[] {"a", "b"}, obj.getIds());
    }

    private static class TestScriptableObject extends ScriptableObject {
        public String getClassName() {
            return "foo";
        }
    }
}
