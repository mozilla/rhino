function createObject(iterations, strings, ints) {
  var o;
  for (var ct = 0; ct < iterations; ct++) {
    o = {};
    var s = 0;
    var i = 0;

    while ((s < strings.length) && (i < ints.length)) {
      if (s < strings.length) {
        o[strings[s]] = strings[s];
        s++;
      }
      if (i < ints.length) {
        o[ints[i]] = ints[i];
        i++;
      }
    }
  }
  return o;
}

function iterateObject(iterations, o) {
  var x;
  for (var ct = 0; ct < iterations; ct++) {
    for (var k in o) {
      x = o[k];
    }
  }
  return x;
}

function iterateOwnKeysObject(iterations, o) {
  var pn;
  for (var ct = 0; ct < iterations; ct++) {
    pn = Object.getOwnPropertyNames(o);
  }
  return pn;
}

function accessObject(iterations, o, strings, ints) {
  var s = 0;
  var i = 0;
  for (var ct = 0; ct < iterations; ct++) {
    if (strings.length > 0) {
      var x = o[strings[s]];
      s++;
      if (s === strings.length) {
        s = 0;
      }
    }
    if (ints.length > 0) {
      var x = o[ints[i]];
      i++;
      if (i === ints.length) {
        i = 0;
      }
    }
  }
}

function deleteObject(iterations, o, strings, ints) {
  var s = 0;
  var i = 0;

  for (var ct = 0; ct < iterations; ct++) {
    if (s < strings.length) {
      delete o[strings[s]];
      s++;
    }
    if (i < ints.length) {
      delete o[ints[i]];
      i++;
    }
  }
}