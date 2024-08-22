load('testsrc/assert.js');

(function TestSymbolSpecies() {
  var symbolSpeciesValue = RegExp[Symbol.species];
  assertEquals(RegExp, symbolSpeciesValue);
})();

'success';
