package org.mozilla.javascript.ast;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Token;

public class ScopeTest {

    private static ScriptNode createTopScope() {
        ScriptNode top = new ScriptNode(0);
        top.top = top;
        return top;
    }

    private static Scope createScope(ScriptNode top) {
        Scope scope = new Scope(0, 10);
        scope.top = top;
        return scope;
    }

    private static Symbol addSymbol(Scope scope, String name, int declType) {
        Symbol sym = new Symbol(declType, name);
        scope.putSymbol(sym);
        return sym;
    }

    @Test
    public void splitScopeMovesSymbolTable() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        addSymbol(original, "x", Token.LET);
        addSymbol(original, "y", Token.LET);

        Scope result = Scope.splitScope(original);

        assertNull(original.getSymbolTable(), "original should have no symbol table after split");
        assertNotNull(result.getSymbolTable(), "result should have the symbol table");
        assertTrue(result.getSymbolTable().containsKey("x"));
        assertTrue(result.getSymbolTable().containsKey("y"));
    }

    @Test
    public void splitScopeUpdatesContainingTable() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        Symbol symX = addSymbol(original, "x", Token.LET);
        Symbol symY = addSymbol(original, "y", Token.LET);

        assertSame(original, symX.getContainingTable());
        assertSame(original, symY.getContainingTable());

        Scope result = Scope.splitScope(original);

        assertSame(
                result,
                symX.getContainingTable(),
                "symbol containingTable should point to the new scope");
        assertSame(
                result,
                symY.getContainingTable(),
                "symbol containingTable should point to the new scope");
    }

    @Test
    public void splitScopeCopiesVarSymbolTable() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        Symbol varSym = new Symbol(Token.VAR, "v");
        original.putSymbol(varSym);
        original.putVarSymbol(varSym);

        Scope result = Scope.splitScope(original);

        assertNotNull(
                original.getVarSymbolTable(),
                "original should keep its varSymbolTable after split");
        assertTrue(original.getVarSymbolTable().containsKey("v"));

        assertNotNull(result.getVarSymbolTable(), "result should have a copy of varSymbolTable");
        assertTrue(result.getVarSymbolTable().containsKey("v"));
    }

    @Test
    public void splitScopeVarSymbolTablesAreIndependent() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        Symbol varSym = new Symbol(Token.VAR, "v");
        original.putSymbol(varSym);
        original.putVarSymbol(varSym);

        Scope result = Scope.splitScope(original);

        // Mutating one should not affect the other
        Symbol varSym2 = new Symbol(Token.VAR, "w");
        original.putVarSymbol(varSym2);

        assertFalse(
                result.getVarSymbolTable().containsKey("w"),
                "result varSymbolTable should be independent of original");

        Map<String, Symbol> origVarTable = original.getVarSymbolTable();
        Map<String, Symbol> resultVarTable = result.getVarSymbolTable();
        assertNotSame(
                origVarTable,
                resultVarTable,
                "varSymbolTable should not be the same object instance");
    }

    @Test
    public void splitScopeWithNullTables() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        // No symbols at all
        Scope result = Scope.splitScope(original);

        assertNull(original.getSymbolTable());
        assertNull(result.getSymbolTable());
        assertNull(original.getVarSymbolTable());
        assertNull(result.getVarSymbolTable());
    }

    @Test
    public void splitScopeSetsParentScopeChain() {
        ScriptNode top = createTopScope();
        Scope original = createScope(top);
        original.setParentScope(top);

        Scope result = Scope.splitScope(original);

        assertSame(top, result.getParentScope(), "result should have original's parent scope");
        // original.parentScope is not changed by splitScope
    }
}
