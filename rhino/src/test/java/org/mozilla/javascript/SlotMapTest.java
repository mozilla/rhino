package org.mozilla.javascript;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SlotMapTest {
    // Random number generator with fixed seed to ensure repeatable tests
    private static final Random rand = new Random(0);

    private static class TestScriptableObject extends ScriptableObject {

        public TestScriptableObject() {
            super();
        }

        public String getClassName() {
            return "foo";
        }
    }

    private final ScriptableObject obj;

    // `SingleSlotMaps` will always have a slot, so we need to record
    // the starting size of our slot map and take that into account
    // when testing with one of those as the initial map.
    private final int startingSize;

    public SlotMapTest(Supplier<SlotMap> mapSupplier) {
        this.obj = new TestScriptableObject();
        this.obj.setMap(mapSupplier.get());
        startingSize = this.obj.getMap().size();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> mapTypes() {
        List<Supplier<SlotMap>> suppliers =
                List.of(
                        () -> SlotMapOwner.EMPTY_SLOT_MAP,
                        () -> new SlotMapOwner.SingleEntrySlotMap(new Slot(new Object(), 0, 0)),
                        () -> new EmbeddedSlotMap(),
                        () -> new HashSlotMap(),
                        () -> SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP,
                        () ->
                                new SlotMapOwner.ThreadSafeSingleEntrySlotMap(
                                        new Slot(new Object(), 0, 0)),
                        () -> new ThreadSafeEmbeddedSlotMap(),
                        () -> new ThreadSafeHashSlotMap());
        return suppliers.stream().map(i -> new Object[] {i}).collect(Collectors.toList());
    }

    @Test
    public void empty() {
        if (startingSize == 0) {
            assertEquals(0, obj.getMap().size());
            assertTrue(obj.getMap().isEmpty());
            assertNull(obj.getMap().query("notfound", 0));
            assertNull(obj.getMap().query(null, 123));
        } else {
            assertEquals(startingSize, obj.getMap().size());
            assertFalse(obj.getMap().isEmpty());
        }
    }

    @Test
    public void crudOneString() {
        assertNull(obj.getMap().query("foo", 0));
        Slot slot = obj.getMap().modify(obj, "foo", 0, 0);
        assertNotNull(slot);
        slot.value = "Testing";
        assertEquals(1 + startingSize, obj.getMap().size());
        assertFalse(obj.getMap().isEmpty());
        Slot newSlot = new Slot(slot);
        obj.getMap().compute(obj, "foo", 0, (k, i, e) -> newSlot);
        Slot foundNewSlot = obj.getMap().query("foo", 0);
        assertEquals("Testing", foundNewSlot.value);
        assertSame(foundNewSlot, newSlot);
        obj.getMap().compute(obj, "foo", 0, (k, ii, e) -> null);
        assertNull(obj.getMap().query("foo", 0));
        assertEquals(0 + startingSize, obj.getMap().size());
        if (startingSize == 0) {
            assertTrue(obj.getMap().isEmpty());
        } else {
            assertFalse(obj.getMap().isEmpty());
        }
    }

    @Test
    public void crudOneIndex() {
        assertNull(obj.getMap().query(null, 11));
        Slot slot = obj.getMap().modify(obj, null, 11, 0);
        assertNotNull(slot);
        slot.value = "Testing";
        assertEquals(1 + startingSize, obj.getMap().size());
        assertFalse(obj.getMap().isEmpty());
        Slot newSlot = new Slot(slot);
        obj.getMap().compute(obj, null, 11, (k, i, e) -> newSlot);
        Slot foundNewSlot = obj.getMap().query(null, 11);
        assertEquals("Testing", foundNewSlot.value);
        assertSame(foundNewSlot, newSlot);
        obj.getMap().compute(obj, null, 11, (k, ii, e) -> null);
        assertNull(obj.getMap().query(null, 11));
        assertEquals(0 + startingSize, obj.getMap().size());
        if (startingSize == 0) {
            assertTrue(obj.getMap().isEmpty());
        } else {
            assertFalse(obj.getMap().isEmpty());
        }
    }

    @Test
    public void computeReplaceSlot() {
        Slot slot = obj.getMap().modify(obj, "one", 0, 0);
        slot.value = "foo";
        Slot newSlot =
                obj.getMap()
                        .compute(
                                obj,
                                "one",
                                0,
                                (k, i, e) -> {
                                    assertEquals(k, "one");
                                    assertEquals(i, 0);
                                    assertNotNull(e);
                                    assertEquals(e.value, "foo");
                                    Slot n = new Slot(e);
                                    n.value = "bar";
                                    return n;
                                });
        assertEquals(newSlot.value, "bar");
        slot = obj.getMap().query("one", 0);
        assertEquals(slot.value, "bar");
        assertEquals(1 + startingSize, obj.getMap().size());
    }

    @Test
    public void computeCreateNewSlot() {
        Slot newSlot =
                obj.getMap()
                        .compute(
                                obj,
                                "one",
                                0,
                                (k, i, e) -> {
                                    assertEquals(k, "one");
                                    assertEquals(i, 0);
                                    assertNull(e);
                                    Slot n = new Slot(k, i, 0);
                                    n.value = "bar";
                                    return n;
                                });
        assertNotNull(newSlot);
        assertEquals(newSlot.value, "bar");
        Slot slot = obj.getMap().query("one", 0);
        assertNotNull(slot);
        assertEquals(slot.value, "bar");
        assertEquals(1 + startingSize, obj.getMap().size());
    }

    @Test
    public void computeRemoveSlot() {
        Slot slot = obj.getMap().modify(obj, "one", 0, 0);
        slot.value = "foo";
        Slot newSlot =
                obj.getMap()
                        .compute(
                                obj,
                                "one",
                                0,
                                (k, i, e) -> {
                                    assertEquals(k, "one");
                                    assertEquals(i, 0);
                                    assertNotNull(e);
                                    assertEquals(e.value, "foo");
                                    return null;
                                });
        assertNull(newSlot);
        slot = obj.getMap().query("one", 0);
        assertNull(slot);
        assertEquals(0 + startingSize, obj.getMap().size());
    }

    private static final int NUM_INDICES = 67;

    @Test
    public void manyKeysAndIndices() {
        for (int i = 0; i < NUM_INDICES; i++) {
            Slot newSlot = obj.getMap().modify(obj, null, i, 0);
            newSlot.value = i;
        }
        for (String key : KEYS) {
            Slot newSlot = obj.getMap().modify(obj, key, 0, 0);
            newSlot.value = key;
        }
        assertEquals(KEYS.length + NUM_INDICES + startingSize, obj.getMap().size());
        assertFalse(obj.getMap().isEmpty());
        verifyIndicesAndKeys();

        // Randomly replace some slots
        for (int i = 0; i < 20; i++) {
            int ix = rand.nextInt(NUM_INDICES);
            Slot slot = obj.getMap().query(null, ix);
            assertNotNull(slot);
            obj.getMap().compute(obj, null, ix, (k, j, e) -> new Slot(slot));
        }
        for (int i = 0; i < 20; i++) {
            int ix = rand.nextInt(KEYS.length);
            Slot slot = obj.getMap().query(KEYS[ix], 0);
            assertNotNull(slot);
            obj.getMap().compute(obj, KEYS[ix], 0, (k, j, e) -> new Slot(slot));
        }
        verifyIndicesAndKeys();

        // Randomly remove slots -- which we do using compute because that's all
        // that ScriptableObject needs.
        HashSet<Integer> removedIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            int ix = rand.nextInt(NUM_INDICES);
            obj.getMap().compute(obj, null, ix, (k, ii, e) -> null);
            removedIds.add(ix);
        }
        HashSet<String> removedKeys = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            int ix = rand.nextInt(NUM_INDICES);
            obj.getMap().compute(obj, KEYS[ix], ix, (k, ii, e) -> null);
            removedKeys.add(KEYS[ix]);
        }

        for (int i = 0; i < NUM_INDICES; i++) {
            Slot slot = obj.getMap().query(null, i);
            if (removedIds.contains(i)) {
                assertNull(slot);
            } else {
                assertNotNull(slot);
                assertEquals(i, slot.value);
            }
        }
        for (String key : KEYS) {
            Slot slot = obj.getMap().query(key, 0);
            if (removedKeys.contains(key)) {
                assertNull(slot);
            } else {
                assertNotNull(slot);
                assertEquals(key, slot.value);
            }
        }
    }

    private void verifyIndicesAndKeys() {
        long lockStamp = 0;
        lockStamp = obj.getMap().readLock();
        try {
            Iterator<Slot> it = obj.getMap().iterator();
            for (int i = 0; i < startingSize; i++) {
                // Skip initial slots
                it.next();
            }
            for (int i = 0; i < NUM_INDICES; i++) {
                Slot slot = obj.getMap().query(null, i);
                assertNotNull(slot);
                assertEquals(i, slot.value);
                assertTrue(it.hasNext());
                assertEquals(slot, it.next());
            }
            for (String key : KEYS) {
                Slot slot = obj.getMap().query(key, 0);
                assertNotNull(slot);
                assertEquals(key, slot.value);
                assertTrue(it.hasNext());
                assertEquals(slot, it.next());
            }
            assertFalse(it.hasNext());
        } finally {
            obj.getMap().unlockRead(lockStamp);
        }
    }

    // These keys come from the hash collision test and may help ensure that we have a few
    // collisions for proper testing of the embedded slot obj.getMap().
    private static final String[] KEYS = {
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaAaBBBBBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaAaBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBAaBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBAaBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBBBAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBAaBBBBBBBBBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAaAaAa",
        "AaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAaAaBB",
        "AaAaAaAaAaAaAaAaAaAaAaBBBBAaAaAaBBAa",
    };
}
