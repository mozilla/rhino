package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlotMapDescriptor<O extends ScriptableObject> {

    private final List<CompactSlot.Descriptor<?, Scriptable, O>> slots;
    private final int[] attributes;

    private SlotMapDescriptor(
            List<CompactSlot.Descriptor<?, Scriptable, O>> slots, int[] attributes) {
        this.slots = slots;
        this.attributes = attributes;
    }

    SlotMap<Scriptable> buildMap(O owner) {
        return SlotMapOwner.createSlotMap(
                slots.size() > 0 ? slots.get(0).createSlot(owner, attributes[0]) : null,
                slots.size() > 1 ? slots.get(1).createSlot(owner, attributes[1]) : null,
                slots.size() > 2 ? slots.get(2).createSlot(owner, attributes[2]) : null,
                slots.size() > 3 ? slots.get(3).createSlot(owner, attributes[3]) : null);
    }

    public void installMap(O owner) {
        owner.setMap(buildMap(owner));
    }

    public static class Builder<O extends ScriptableObject> {
        List<CompactSlot.Descriptor<?, Scriptable, O>> slots = new ArrayList<>();
        List<Integer> attributes = new ArrayList<>();

        public Builder() {}

        private Builder(SlotMapDescriptor<O> old) {
            slots = new ArrayList<>(old.slots);
            attributes = new ArrayList<>(Arrays.stream(old.attributes).mapToObj(v -> v).toList());
        }

        public static <O extends ScriptableObject> Builder<O> extending(
                SlotMapDescriptor<O> start) {
            return new Builder<>(start);
        }

        public Builder<O> withSlot(
                CompactSlot.Descriptor<?, Scriptable, O> descriptor, int attributes) {
            if (slots.size() > 4) {
                throw new IllegalStateException("Only maps of size 4 or less supported.");
            }
            slots.add(descriptor);
            this.attributes.add(attributes);
            return this;
        }

        public SlotMapDescriptor<O> build() {
            return new SlotMapDescriptor<>(
                    List.copyOf(slots), attributes.stream().mapToInt(v -> v).toArray());
        }
    }
}
