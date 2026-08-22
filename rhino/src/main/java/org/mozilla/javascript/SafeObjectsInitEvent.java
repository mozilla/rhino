package org.mozilla.javascript;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Name;
import jdk.jfr.Registered;

@Category({"Mozilla", "Rhino"})
@Name("org.mozilla.javascript.SafeObjectsInitEvent")
@Description("Safe objects initialisation")
@Registered(true)
@Enabled(false)
public class SafeObjectsInitEvent extends JFREmitter.RhinoEvent {
    public SafeObjectsInitEvent() {
        super();
        begin();
    }

    @Override
    void fillAndSubmit(Object... data) {
        if (this.shouldCommit()) {
            this.commit();
        }
    }
}
