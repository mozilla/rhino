var result = (function () { 
class C extends null {
  constructor() { return Object.create(null); }
}
return Function.prototype.isPrototypeOf(C)
  && Object.getPrototypeOf(C.prototype) === null;
       })();
if (result) {
    print("[SUCCESS]");
}
