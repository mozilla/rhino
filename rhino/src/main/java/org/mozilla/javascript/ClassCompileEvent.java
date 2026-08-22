package org.mozilla.javascript;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Registered;

@Category({"Mozilla", "Rhino"})
@Name("org.mozilla.javascript.ClassCompileEvent")
@Description("Class compilation")
@Registered(true)
@Enabled(false)
public class ClassCompileEvent extends JFREmitter.RhinoEvent {
    @Label("Source Name")
    public String name;

    ClassCompileEvent() {
        super();
        begin();
    }

    @Override
    void fillAndSubmit(Object... data) {
        if (this.shouldCommit()) {
            name = (String) data[0];
            this.commit();
        }
    }
}
