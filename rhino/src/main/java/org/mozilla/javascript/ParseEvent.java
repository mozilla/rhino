package org.mozilla.javascript;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Registered;

@Category({"Mozilla", "Rhino"})
@Name("org.mozilla.javascript.ParseEvent")
@Description("Script parsing")
@Registered(true)
@Enabled(false)
public class ParseEvent extends JFREmitter.RhinoEvent {
    @Label("Source Name")
    public String name;

    @Label("Source Length")
    public int length;

    public ParseEvent() {
        super();
        begin();
    }

    @Override
    void fillAndSubmit(Object... data) {
        if (this.shouldCommit()) {
            name = (String) data[0];
            length = (Integer) data[1];
            this.commit();
        }
    }
}
