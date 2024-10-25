package org.mozilla.javascript.optimizer;

import jdk.dynalink.NamedOperation;
import jdk.dynalink.Namespace;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.Operation;

@SuppressWarnings("AndroidJdkLibsChecker")
class ParsedOperation {
    private final Operation root;
    private final Namespace namespace;
    private final String name;
    private final Operation operation;

    ParsedOperation(Operation rootOp) {
        this.root = rootOp;
        // Many, but not all, operations in our system are named operations
        Object nameObj = NamedOperation.getName(rootOp);
        if (nameObj instanceof String) {
            this.name = (String) nameObj;
        } else if (nameObj != null) {
            throw new UnsupportedOperationException(rootOp.toString());
        } else {
            this.name = "";
        }

        // All operations in our system are namespace operations with one namespace
        Operation op = NamedOperation.getBaseOperation(rootOp);
        assert op instanceof NamespaceOperation;
        NamespaceOperation nsOp = (NamespaceOperation) op;
        assert nsOp.getNamespaceCount() == 1;
        this.namespace = nsOp.getNamespace(0);
        this.operation = nsOp.getBaseOperation();
    }

    boolean isNamespace(Namespace ns) {
        return ns.equals(namespace);
    }

    boolean isOperation(Operation op) {
        return op.equals(operation);
    }

    boolean isOperation(Operation op1, Operation op2) {
        return op1.equals(operation) || op2.equals(operation);
    }

    String getName() {
        return name;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
