package org.mozilla.javascript;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Name;
import jdk.jfr.Registered;

@Category({"Mozilla", "Rhino"})
@Name("org.mozilla.javascript.ObjectsInitEvent")
@Description("Safe objects initialisation")
@Registered(true)
@Enabled(false)
public class ObjectsInitEvent extends JFREmitter.RhinoEvent {
    public ObjectsInitEvent() {
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
