# Kotlin Support

The rhino-kotlin module uses Kotlin metadata to augment function and property information,
making JavaScript APIs more nuanced.

For example, the following code exposes setValue JavaScript function:
```
defineProperty(
    topLevelScope,
    "setValue",
    Bundle::class.java.methods.find {
        it.name == "setValue"
    },
    DONT_ENUM
)
```
which is backed by the following Kotlin class:
```
class Bundle() : ScriptableObject {
    val valueMap = emptyMap<String, String?>()

    fun setValue(key: String, value: String?) {
        valueMap[key] = value
    }
}
```
Imagine rhino-kotlin is not used and JavaScript code tries to call setValue with `null` value parameter:
```
setValue("key", null)
```
This will lead to unexpected result - a 4-char long string "null" will be inserted into the backing map.
This happens because Rhino engine doesn't know how to infer parameter nullability from pure Java code,
so it tries to convert `null` to String.

Adding rhino-kotlin dependency fixes the problem, allowing JavaScript functions to have nullable parameters.

At the moment, only parameter nullability is supported, but we might add more Kotlin-specific support in future.

**Note:** If building for Android, make sure the kotlin.Metadata class is not obfuscated by adding the following line
into the proguard configuration:
```
-keep class kotlin.Metadata
```
Without this line Rhino Kotlin support won't work in release apk.
