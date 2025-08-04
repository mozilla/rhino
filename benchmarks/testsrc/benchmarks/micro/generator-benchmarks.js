function* myGenerator(limit) {
    for (i = 0; i < limit; i++) {
        yield i;
    }
    return -1;
}

function _ts_generator(thisArg, body) {
    var f, y, t, g, _ = {
        label: 0,
        sent: function() {
            if (t[0] & 1) throw t[1];
            return t[1];
        },
        trys: [],
        ops: []
    };
    return g = {
        next: verb(0),
        "throw": verb(1),
        "return": verb(2)
    }, typeof Symbol === "function" && (g[Symbol.iterator] = function() {
        return this;
    }), g;
    function verb(n) {
        return function(v) {
            return step([
                n,
                v
            ]);
        };
    }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while(_)try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [
                op[0] & 2,
                t.value
            ];
            switch(op[0]){
                case 0:
                case 1:
                    t = op;
                    break;
                case 4:
                    _.label++;
                    return {
                        value: op[1],
                        done: false
                    };
                case 5:
                    _.label++;
                    y = op[1];
                    op = [
                        0
                    ];
                    continue;
                case 7:
                    op = _.ops.pop();
                    _.trys.pop();
                    continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) {
                        _ = 0;
                        continue;
                    }
                    if (op[0] === 3 && (!t || op[1] > t[0] && op[1] < t[3])) {
                        _.label = op[1];
                        break;
                    }
                    if (op[0] === 6 && _.label < t[1]) {
                        _.label = t[1];
                        t = op;
                        break;
                    }
                    if (t && _.label < t[2]) {
                        _.label = t[2];
                        _.ops.push(op);
                        break;
                    }
                    if (t[2]) _.ops.pop();
                    _.trys.pop();
                    continue;
            }
            op = body.call(thisArg, _);
        } catch (e) {
            op = [
                6,
                e
            ];
            y = 0;
        } finally{
            f = t = 0;
        }
        if (op[0] & 5) throw op[1];
        return {
            value: op[0] ? op[1] : void 0,
            done: true
        };
    }
}

function myGenerator2(limit) {
    return _ts_generator(this, function(_state) {
        switch(_state.label){
            case 0:
                i = 0;
                _state.label = 1;
            case 1:
                if (!(i < limit)) return [
                    3,
                    4
                ];
                return [
                    4,
                    i
                ];
            case 2:
                _state.sent();
                _state.label = 3;
            case 3:
                i++;
                return [
                    3,
                    1
                ];
            case 4:
                return [
                    2,
                    -1
                ];
        }
    });
}

function nativeGenerator() {
    const gen = myGenerator(1_000);
    count = 0;
    while (! (gen.next().done)) {
        count++;
    }
    return count;
}

function transpiledGenerator() {
    const gen = myGenerator2(1_000);
    count = 0;
    while (! (gen.next().done)) {
        count++;
    }
    return count;
}

function noReturnGenerator() {
    const gen = myGenerator(1_000_000);
    count = 0;
    while (gen.next().value <= 1_000) {
        count++;
    }
    return count;
}

nativeGenerator();
transpiledGenerator();
noReturnGenerator();
