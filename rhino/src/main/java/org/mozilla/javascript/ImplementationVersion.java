package org.mozilla.javascript;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This class is a singleton that just exists to serve up the implementation version. This should
 * encourage that it's safely but lazily loaded just once per VM.
 */
public class ImplementationVersion {

    private String versionString;

    private static final ImplementationVersion version = new ImplementationVersion();

    public static String get() {
        return version.versionString;
    }

    private ImplementationVersion() {
        Enumeration<URL> urls;
        try {
            urls =
                    ImplementationVersion.class
                            .getClassLoader()
                            .getResources("META-INF/MANIFEST.MF");
        } catch (IOException ioe) {
            return;
        }

        // There will be many manifests in the world -- enumerate all of them until we find the
        // right one.
        while (urls.hasMoreElements()) {
            URL metaUrl = urls.nextElement();
            try (InputStream is = metaUrl.openStream()) {
                Manifest mf = new Manifest(is);
                Attributes attrs = mf.getMainAttributes();
                if ("Mozilla Rhino".equals(attrs.getValue("Implementation-Title"))) {
                    StringBuilder buf = new StringBuilder(23);
                    buf.append("Rhino ").append(attrs.getValue("Implementation-Version"));
                    String builtDate = attrs.getValue("Built-Date");
                    if (builtDate != null) {
                        builtDate = builtDate.replaceAll("-", " ");
                        buf.append(' ').append(builtDate);
                    }
                    versionString = buf.toString();
                    return;
                }
            } catch (IOException e) {
                // Ignore this unlikely event
            }
        }
        // We are probably in a IDE
        versionString = "Rhino Snapshot";
    }
}
