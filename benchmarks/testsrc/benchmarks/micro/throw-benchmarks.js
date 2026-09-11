function f(depth) {
    try {
        g(depth);
    } catch (e) {
        return -1;
    }
}

function g(depth) {
    if (depth <= 0) {
        throw new Error("depth reached!");
    }
    g(depth - 1.0);
}

function shallowThrow() {
    f(0.0);
}

function mediumThrow() {
    f(10.0);
}

function deepThrow() {
    f(100.0);
}

// The same throws, but reading the stack. Construction cost is paid either way; these
// additionally pay for rendering the JavaScript stack out of the captured frames.
function fRead(depth) {
    try {
        g(depth);
    } catch (e) {
        return e.stack.length;
    }
}

function shallowThrowRead() {
    return fRead(0.0);
}

function mediumThrowRead() {
    return fRead(10.0);
}

function deepThrowRead() {
    return fRead(100.0);
}

shallowThrow();
mediumThrow();
deepThrow();
shallowThrowRead();
mediumThrowRead();
deepThrowRead();
