load("testsrc/assert.js");

// Test ArrayBuffer.prototype.transfer and transferToFixedLength integration scenarios

(function TestTransferWithTypedArrayViews() {
    var buffer = new ArrayBuffer(16);
    var uint8 = new Uint8Array(buffer);
    var float32 = new Float32Array(buffer);
    
    // Set some data
    uint8[0] = 42;
    uint8[1] = 24;
    float32[1] = 3.14;
    
    var transferred = buffer.transfer();
    
    // Original buffer should be detached
    assertTrue(buffer.detached);
    assertEquals(0, buffer.byteLength);
    
    // Views should be detached too
    assertThrows(function() { uint8[0]; }, TypeError);
    assertThrows(function() { float32[0]; }, TypeError);
    
    // New buffer should have the data
    var newUint8 = new Uint8Array(transferred);
    assertEquals(42, newUint8[0]);
    assertEquals(24, newUint8[1]);
})();

(function TestTransferWithDataView() {
    var buffer = new ArrayBuffer(16);
    var view = new DataView(buffer);
    
    view.setUint8(0, 255);
    view.setFloat32(4, 2.5, true);
    view.setInt16(8, -1000, false);
    
    var transferred = buffer.transfer();
    
    // Original DataView should be detached
    assertThrows(function() { view.getUint8(0); }, TypeError);
    
    // Create new DataView and verify data
    var newView = new DataView(transferred);
    assertEquals(255, newView.getUint8(0));
    assertEquals(2.5, newView.getFloat32(4, true));
    assertEquals(-1000, newView.getInt16(8, false));
})();

(function TestTransferToFixedLengthShrinking() {
    var buffer = new ArrayBuffer(20);
    var uint8 = new Uint8Array(buffer);
    
    // Fill with test pattern
    for (var i = 0; i < 20; i++) {
        uint8[i] = i * 2;
    }
    
    var transferred = buffer.transferToFixedLength(10);
    
    assertEquals(10, transferred.byteLength);
    var newUint8 = new Uint8Array(transferred);
    
    // Should have first 10 bytes
    for (var i = 0; i < 10; i++) {
        assertEquals(i * 2, newUint8[i]);
    }
})();

(function TestTransferToFixedLengthGrowing() {
    var buffer = new ArrayBuffer(8);
    var uint8 = new Uint8Array(buffer);
    uint8[0] = 100;
    uint8[7] = 200;
    
    var transferred = buffer.transferToFixedLength(16);
    
    assertEquals(16, transferred.byteLength);
    var newUint8 = new Uint8Array(transferred);
    
    // Original data should be preserved
    assertEquals(100, newUint8[0]);
    assertEquals(200, newUint8[7]);
    
    // New bytes should be zero
    assertEquals(0, newUint8[8]);
    assertEquals(0, newUint8[15]);
})();

(function TestTransferWithMultipleViewTypes() {
    var buffer = new ArrayBuffer(32);
    var uint8 = new Uint8Array(buffer);
    var uint16 = new Uint16Array(buffer, 8, 4);
    var float32 = new Float32Array(buffer, 16, 4);
    var dataView = new DataView(buffer, 0, 32);
    
    // Set test data
    uint8[0] = 0xFF;
    uint16[0] = 0x1234;
    float32[0] = Math.PI;
    dataView.setUint32(24, 0xDEADBEEF, true);
    
    var transferred = buffer.transfer();
    
    // All views should be detached
    assertThrows(function() { return uint8[0]; }, TypeError);
    assertThrows(function() { return uint16[0]; }, TypeError);
    assertThrows(function() { return float32[0]; }, TypeError);
    assertThrows(function() { return dataView.getUint8(0); }, TypeError);
    
    // Verify transferred data
    var newDataView = new DataView(transferred);
    assertEquals(0xFF, newDataView.getUint8(0));
    assertEquals(0x1234, newDataView.getUint16(8, true));
    assertEqualsDelta(Math.PI, newDataView.getFloat32(16, true), 0.0001);
    assertEquals(0xDEADBEEF, newDataView.getUint32(24, true));
})();

(function TestTransferErrorConditions() {
    var buffer = new ArrayBuffer(16);
    
    // Transfer with invalid lengths
    assertThrows(function() { buffer.transfer(-1); }, RangeError);
    assertThrows(function() { buffer.transferToFixedLength(-5); }, RangeError);
    
    // Transfer with Infinity should throw
    assertThrows(function() { buffer.transfer(Infinity); }, RangeError);
    assertThrows(function() { buffer.transfer(-Infinity); }, RangeError);
    assertThrows(function() { buffer.transferToFixedLength(Infinity); }, RangeError);
    assertThrows(function() { buffer.transferToFixedLength(-Infinity); }, RangeError);
    
    // Transfer with NaN should be treated as 0
    var nanBuffer1 = new ArrayBuffer(16);
    var transferred1 = nanBuffer1.transfer(NaN);
    assertEquals(0, transferred1.byteLength);
    
    var nanBuffer2 = new ArrayBuffer(16);
    var transferred2 = nanBuffer2.transferToFixedLength(NaN);
    assertEquals(0, transferred2.byteLength);
    
    // Transfer valid buffer
    var transferred = buffer.transfer();
    
    // Original buffer is now detached
    assertThrows(function() { buffer.transfer(); }, TypeError);
    assertThrows(function() { buffer.transferToFixedLength(10); }, TypeError);
    assertThrows(function() { buffer.slice(0, 8); }, TypeError);
})();

(function TestTransferZeroLength() {
    var buffer = new ArrayBuffer(0);
    var transferred = buffer.transfer();
    
    assertEquals(0, transferred.byteLength);
    assertTrue(buffer.detached);
    
    var buffer2 = new ArrayBuffer(0);
    var transferred2 = buffer2.transferToFixedLength(0);
    assertEquals(0, transferred2.byteLength);
})();

(function TestTransferPreservesBufferIdentity() {
    var buffer = new ArrayBuffer(16);
    var uint8 = new Uint8Array(buffer);
    uint8[0] = 123;
    
    var transferred = buffer.transfer(16);
    
    // Should be different objects
    assertFalse(buffer === transferred);
    // But should have the same constructor and prototype
    assertTrue(transferred.constructor === ArrayBuffer);
    assertTrue(Object.getPrototypeOf(transferred) === ArrayBuffer.prototype);
    
    // But data should be the same
    var newUint8 = new Uint8Array(transferred);
    assertEquals(123, newUint8[0]);
})();

(function TestTransferWithUndefinedLength() {
    var buffer = new ArrayBuffer(20);
    var uint8 = new Uint8Array(buffer);
    uint8[19] = 42;
    
    // transfer() with undefined should preserve length
    var transferred1 = buffer.transfer(undefined);
    assertEquals(20, transferred1.byteLength);
    
    var buffer2 = new ArrayBuffer(15);
    var uint8_2 = new Uint8Array(buffer2);
    uint8_2[14] = 99;
    
    // transferToFixedLength() with undefined should preserve length
    var transferred2 = buffer2.transferToFixedLength(undefined);
    assertEquals(15, transferred2.byteLength);
    
    var newUint8 = new Uint8Array(transferred2);
    assertEquals(99, newUint8[14]);
})();

(function TestTransferMethodProperties() {
    // Test method length property
    assertEquals(0, ArrayBuffer.prototype.transfer.length);
    assertEquals(0, ArrayBuffer.prototype.transferToFixedLength.length);
    
    // Test method name property
    assertEquals("transfer", ArrayBuffer.prototype.transfer.name);
    assertEquals("transferToFixedLength", ArrayBuffer.prototype.transferToFixedLength.name);
    
    // Methods should not be enumerable
    var buffer = new ArrayBuffer(8);
    var props = Object.getOwnPropertyNames(ArrayBuffer.prototype);
    assertTrue(props.indexOf("transfer") !== -1);
    assertTrue(props.indexOf("transferToFixedLength") !== -1);
    
    // But should not appear in for...in
    var enumProps = [];
    for (var prop in ArrayBuffer.prototype) {
        enumProps.push(prop);
    }
    assertFalse(enumProps.indexOf("transfer") !== -1);
    assertFalse(enumProps.indexOf("transferToFixedLength") !== -1);
})();

(function TestTransferWithBigData() {
    // Test with larger buffer to ensure proper copying
    var size = 1024;
    var buffer = new ArrayBuffer(size);
    var uint8 = new Uint8Array(buffer);
    
    // Create a pattern
    for (var i = 0; i < size; i++) {
        uint8[i] = i % 256;
    }
    
    var transferred = buffer.transfer();
    var newUint8 = new Uint8Array(transferred);
    
    // Verify entire pattern
    for (var i = 0; i < size; i++) {
        assertEquals(i % 256, newUint8[i]);
    }
    
    // Test shrinking large buffer
    var buffer2 = new ArrayBuffer(1024);
    var uint8_2 = new Uint8Array(buffer2);
    for (var i = 0; i < 1024; i++) {
        uint8_2[i] = (i * 3) % 256;
    }
    
    var transferred2 = buffer2.transferToFixedLength(512);
    var newUint8_2 = new Uint8Array(transferred2);
    
    assertEquals(512, transferred2.byteLength);
    for (var i = 0; i < 512; i++) {
        assertEquals((i * 3) % 256, newUint8_2[i]);
    }
})();

(function TestTransferConsistentBehavior() {
    // Ensure transfer() and transferToFixedLength() behave consistently
    var buffer1 = new ArrayBuffer(16);
    var buffer2 = new ArrayBuffer(16);
    
    var uint8_1 = new Uint8Array(buffer1);
    var uint8_2 = new Uint8Array(buffer2);
    
    // Set same data
    for (var i = 0; i < 16; i++) {
        uint8_1[i] = i + 100;
        uint8_2[i] = i + 100;
    }
    
    var transferred1 = buffer1.transfer(16);
    var transferred2 = buffer2.transferToFixedLength(16);
    
    // Both should produce identical results
    var newUint8_1 = new Uint8Array(transferred1);
    var newUint8_2 = new Uint8Array(transferred2);
    
    assertEquals(16, transferred1.byteLength);
    assertEquals(16, transferred2.byteLength);
    
    for (var i = 0; i < 16; i++) {
        assertEquals(newUint8_1[i], newUint8_2[i]);
        assertEquals(i + 100, newUint8_1[i]);
    }
})();

(function TestTransferWithDifferentTypedArrays() {
    var buffer = new ArrayBuffer(64);
    
    // Test with different typed array types
    var int8 = new Int8Array(buffer, 0, 8);
    var uint16 = new Uint16Array(buffer, 8, 4);
    var int32 = new Int32Array(buffer, 16, 4);
    var float64 = new Float64Array(buffer, 32, 4);
    
    // Set test values
    int8[0] = -128;
    int8[7] = 127;
    uint16[0] = 65535;
    int32[0] = -2147483648;
    float64[0] = Math.E;
    
    var transferred = buffer.transfer();
    
    // Verify all views are detached
    assertThrows(function() { return int8[0]; }, TypeError);
    assertThrows(function() { return uint16[0]; }, TypeError);
    assertThrows(function() { return int32[0]; }, TypeError);
    assertThrows(function() { return float64[0]; }, TypeError);
    
    // Create new views and verify data
    var newInt8 = new Int8Array(transferred, 0, 8);
    var newUint16 = new Uint16Array(transferred, 8, 4);
    var newInt32 = new Int32Array(transferred, 16, 4);
    var newFloat64 = new Float64Array(transferred, 32, 4);
    
    assertEquals(-128, newInt8[0]);
    assertEquals(127, newInt8[7]);
    assertEquals(65535, newUint16[0]);
    assertEquals(-2147483648, newInt32[0]);
    assertEqualsDelta(Math.E, newFloat64[0], 0.0000001);
})();

(function TestTransferMemoryAlignment() {
    // Test transfer with various alignment scenarios
    var sizes = [1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33];
    
    for (var i = 0; i < sizes.length; i++) {
        var size = sizes[i];
        var buffer = new ArrayBuffer(size);
        var uint8 = new Uint8Array(buffer);
        
        // Fill with pattern
        for (var j = 0; j < size; j++) {
            uint8[j] = (j * 7) % 256;
        }
        
        var transferred = buffer.transfer();
        assertEquals(size, transferred.byteLength);
        
        var newUint8 = new Uint8Array(transferred);
        for (var j = 0; j < size; j++) {
            assertEquals((j * 7) % 256, newUint8[j]);
        }
    }
})();

(function TestTransferWithArrayBufferIsView() {
    var buffer = new ArrayBuffer(16);
    var uint8 = new Uint8Array(buffer);
    var dataView = new DataView(buffer);
    
    // These should be views
    assertTrue(ArrayBuffer.isView(uint8));
    assertTrue(ArrayBuffer.isView(dataView));
    assertFalse(ArrayBuffer.isView(buffer));
    
    var transferred = buffer.transfer();
    
    // After transfer, original views should still be ArrayBuffer views
    // (even though they're detached)
    assertTrue(ArrayBuffer.isView(uint8));
    assertTrue(ArrayBuffer.isView(dataView));
    assertFalse(ArrayBuffer.isView(transferred));
    
    // New views on transferred buffer should be views
    var newUint8 = new Uint8Array(transferred);
    assertTrue(ArrayBuffer.isView(newUint8));
})();

(function TestTransferDoesNotAffectPrototype() {
    var buffer = new ArrayBuffer(8);
    
    // Ensure prototype methods are still available after transfer
    assertTrue(typeof buffer.transfer === 'function');
    assertTrue(typeof buffer.transferToFixedLength === 'function');
    assertTrue(typeof buffer.slice === 'function');
    
    var transferred = buffer.transfer();
    
    // Transferred buffer should have same prototype methods
    assertTrue(typeof transferred.transfer === 'function');
    assertTrue(typeof transferred.transferToFixedLength === 'function');
    assertTrue(typeof transferred.slice === 'function');
    
    // Original buffer should still have methods (but they throw when called)
    assertTrue(typeof buffer.transfer === 'function');
    assertTrue(typeof buffer.transferToFixedLength === 'function');
    assertTrue(typeof buffer.slice === 'function');
})();

print("ArrayBuffer transfer integration tests completed successfully");