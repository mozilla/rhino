package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TypedArraysLoader;

/**
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class TypedArraysLoaderImpl implements TypedArraysLoader {
    @Override
    public void load(ScriptableObject scope, boolean sealed) {
        new LazilyLoadedCtor(scope, "ArrayBuffer", sealed, true, NativeArrayBuffer::init);
        new LazilyLoadedCtor(scope, "Int8Array", sealed, true, NativeInt8Array::init);
        new LazilyLoadedCtor(scope, "Uint8Array", sealed, true, NativeUint8Array::init);
        new LazilyLoadedCtor(
                scope, "Uint8ClampedArray", sealed, true, NativeUint8ClampedArray::init);
        new LazilyLoadedCtor(scope, "Int16Array", sealed, true, NativeInt16Array::init);
        new LazilyLoadedCtor(scope, "Uint16Array", sealed, true, NativeUint16Array::init);
        new LazilyLoadedCtor(scope, "Int32Array", sealed, true, NativeInt32Array::init);
        new LazilyLoadedCtor(scope, "Uint32Array", sealed, true, NativeUint32Array::init);
        new LazilyLoadedCtor(scope, "Float32Array", sealed, true, NativeFloat32Array::init);
        new LazilyLoadedCtor(scope, "Float64Array", sealed, true, NativeFloat64Array::init);
        new LazilyLoadedCtor(scope, "DataView", sealed, true, NativeDataView::init);
    }
}
