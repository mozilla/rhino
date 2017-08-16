package org.mozilla.javascript;

public class ES6IteratorResult
    extends IdScriptableObject
{
    private static final Object RESULT_TAG = "@@IteratorResult";

    private Object value = Undefined.instance;
    private boolean done;

    @Override
    public String getClassName() {
        return "ES6IteratorResult";
    }

    static ES6IteratorResult init(ScriptableObject scope, boolean sealed) {

        ES6IteratorResult prototype = new ES6IteratorResult();
        if (scope != null) {
            prototype.setParentScope(scope);
            prototype.setPrototype(getObjectPrototype(scope));
        }
        //prototype.activatePrototypeMap(0);
        if (sealed) {
            prototype.sealObject();
        }

        // Need to access Generator prototype when constructing
        // Generator instances, but don't have a generator constructor
        // to use to find the prototype. Use the "associateValue"
        // approach instead.
        if (scope != null) {
            scope.associateValue(RESULT_TAG, prototype);
        }

        return prototype;
    }

    private ES6IteratorResult()
    {
    }

    ES6IteratorResult(Scriptable scope)
    {
        // Set parent and prototype properties. Since we don't have a
        // constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        IdScriptableObject prototype = (IdScriptableObject)
                ScriptableObject.getTopScopeValue(top, RESULT_TAG);
        setPrototype(prototype);
    }

    public void setValue(Object v) {
        this.value = v;
    }

    public void setDone(boolean d) {
        this.done = d;
    }

    @Override
    protected String getInstanceIdName(int id)
    {
        switch (id) {
            case Id_value:
                return "value";
            case Id_done:
                return "done";
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id) {
            case Id_value:
                return value;
            case Id_done:
                return done;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

// #string_id_map#

    @Override
    protected int findInstanceIdInfo(String s)
    {
        int id = 0;
// #generated# Last update: 2017-08-04 17:47:18 PDT
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==4) { X="done";id=Id_done; }
            else if (s_length==5) { X="value";id=Id_value; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#

        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        return instanceIdInfo(READONLY, id);
    }

    private static final int
            Id_value            = 1,
            Id_done             = 2,
            MAX_INSTANCE_ID     = Id_done;

// #/string_id_map#

    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }
}
