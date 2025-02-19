import org.mozilla.javascript.NullabilityDetector;
import org.mozilla.kotlin.KotlinNullabilityDetector;

module org.mozilla.rhino.kotlin {
    requires kotlin.metadata.jvm;
    requires kotlin.stdlib;
    requires org.mozilla.rhino;

    exports org.mozilla.kotlin;

    provides NullabilityDetector with
            KotlinNullabilityDetector;
}
