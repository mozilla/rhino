load("testsrc/assert.js");

const regexp = new RegExp();
assertEquals('{}', JSON.stringify(regexp))

const regExpWithProperties = new RegExp();
regExpWithProperties.color = 'red';
assertEquals('{"color":"red"}', JSON.stringify(regExpWithProperties));

"success";