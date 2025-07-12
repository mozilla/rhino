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

shallowThrow();
mediumThrow();
deepThrow();
