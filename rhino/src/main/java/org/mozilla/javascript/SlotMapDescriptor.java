package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

public class SlotMapDescriptor<U extends PropHolder<U>, O extends ScriptableObject> {

    private final List<CompactSlot.Descriptor<?, U, O>> slots;
    private final int[] attributes;

    private SlotMapDescriptor(List<CompactSlot.Descriptor<?, U, O>> slots, int[] attributes) {
        this.slots = slots;
        this.attributes = attributes;
    }

    SlotMap<U> buildMap(O owner) {
        return SlotMapOwner.createSlotMap(
                slots.size() > 0 ? slots.get(0).createSlot(owner, attributes[0]) : null,
                slots.size() > 1 ? slots.get(1).createSlot(owner, attributes[1]) : null,
                slots.size() > 2 ? slots.get(2).createSlot(owner, attributes[2]) : null,
                slots.size() > 3 ? slots.get(3).createSlot(owner, attributes[3]) : null);
    }

    public static class Builder<U extends PropHolder<U>, O extends ScriptableObject> {
        List<CompactSlot.Descriptor<?, U, O>> slots = new ArrayList<>();
        List<Integer> attributes = new ArrayList<>();

        Builder<U, O> withSlot(CompactSlot.Descriptor<?, U, O> descriptor, int attributes) {
            if (slots.size() > 4) {
                throw new IllegalStateException("Only maps of size 4 or less supported.");
            }
            slots.add(descriptor);
            this.attributes.add(attributes);
            return this;
        }

        SlotMapDescriptor<U, O> build() {
            return new SlotMapDescriptor<>(
                    List.copyOf(slots), attributes.stream().mapToInt(v -> v).toArray());
        }
    }
}
