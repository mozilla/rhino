package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.URIParameter;
import java.util.Enumeration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.JavaPolicySecurity;

/** Perform some tests when we have a securityController in place. */
public class SecurityControllerTest {

    private static ProtectionDomain UNTRUSTED_JAVASCRIPT;
    private static ProtectionDomain ALLOW_IMPL_ACCESS;
    private static ProtectionDomain RESTRICT_IMPL_ACCESS;
    protected final Global global = new Global();

    /** Sets up the security manager and loads the "grant-all-java.policy". */
    static void setupSecurityManager() {}
    /** Setup the security */
    @BeforeClass
    public static void setup() throws Exception {
        URL url = SecurityControllerTest.class.getResource("grant-all-java.policy");
        if (url != null) {
            System.setProperty("java.security.policy", url.toString());
            Policy.getPolicy().refresh();
            System.setSecurityManager(new SecurityManager());
        }
        SecurityController.initGlobal(new JavaPolicySecurity());

        url = SecurityControllerTest.class.getResource("javascript.policy");
        Policy policy = Policy.getInstance("JavaPolicy", new URIParameter(url.toURI()));
        RESTRICT_IMPL_ACCESS = createProtectionDomain(policy, "RESTRICT_IMPL_ACCESS");
        ALLOW_IMPL_ACCESS = createProtectionDomain(policy, "ALLOW_IMPL_ACCESS");
    }

    /** Creates a new protectionDomain with the given Code-Source Suffix. */
    private static ProtectionDomain createProtectionDomain(Policy policy, String csSuffix)
            throws MalformedURLException {
        File file = new File(System.getProperty("user.dir"));
        file = new File(file, "javascript");
        file = new File(file, csSuffix);
        URL url = file.toURI().toURL();
        CodeSource cs = new CodeSource(url, (java.security.cert.Certificate[]) null);
        Permissions perms = new Permissions();
        Enumeration<Permission> elems = policy.getPermissions(cs).elements();
        while (elems.hasMoreElements()) {
            perms.add(elems.nextElement());
        }
        perms.setReadOnly();
        return new ProtectionDomain(cs, perms, null, null);
    }

    @Test
    public void barAccess() {
        // f.create produces "SomeClass extends ArrayList<String> implements
        // SomeInterface"
        // we may access array methods, like 'size' defined by ArrayList,
        // but not methods like 'bar' defined by SomeClass, because it is in a restricted package
        String script =
                "f = new com.example.securitytest.SomeFactory();\n"
                        + "var i = f.create();\n"
                        + "i.size();\n"
                        + "i.bar();";

        // try in allowed scope
        runScript(script, ALLOW_IMPL_ACCESS);

        try {
            // in restricted scope, we expect an EcmaError
            runScript(script, RESTRICT_IMPL_ACCESS);
            fail("EcmaError expected");
        } catch (EcmaError ee) {
            assertEquals("TypeError: Cannot find function bar in object []. (#4)", ee.getMessage());
        }

        // try in allowed scope again
        runScript(script, ALLOW_IMPL_ACCESS);
    }

    /**
     * This classShutter checks the "rhino.visible.{pkg}" runtime property, which can be defined in
     * a policy file. Note: Every other code in your stack-chain will need this permission also.
     */
    private static class PolicyClassShutter implements ClassShutter {

        @Override
        public boolean visibleToScripts(String fullClassName) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                int idx = fullClassName.lastIndexOf('.');
                if (idx != -1) {
                    String pkg = fullClassName.substring(0, idx);
                    sm.checkPermission(new RuntimePermission("rhino.visible." + pkg));
                }
            }
            return true;
        }
    }

    /** Compiles and runs the script with the given protection domain. */
    private void runScript(String scriptSourceText, ProtectionDomain pd) {
        Utils.runWithAllOptimizationLevels(
                context -> {
                    context.setClassShutter(new PolicyClassShutter());
                    Scriptable scope = context.initStandardObjects(global);

                    return context.evaluateString(scope, scriptSourceText, "", 1, pd);
                });
    }
}
