package org.mozilla.javascript.tests.type_info;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * @author ZZZank
 */
public class TypeInfoTest {

    /// @see TypesToTest#primitives(float, double, byte, short, int, long, char, boolean)
    @Test
    public void primitives() {
        for (var pack : get("primitives")) {
            test(pack, TypeInfoTestActions::defaultTestAction);
        }
    }

    public static void test(TypePack pack, List<Consumer<TypePack>> action) {
        action.forEach(act -> act.accept(pack));
    }

    @SafeVarargs
    public static void test(TypePack pack, Consumer<TypePack>... actions) {
        test(pack, Arrays.asList(actions));
    }

    private static List<TypePack> get(String name) {
        return Objects.requireNonNull(TypesToTest.ALL.get(name));
    }
}
