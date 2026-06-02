package org.mozilla.javascript.interpreterv2;

import java.io.Serializable;
import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeContinuation;

public final class ContinuationJump implements Serializable {
    private static final long serialVersionUID = 7687739156004308247L;

    public CallFrameV2 capturedFrame;
    public CallFrameV2 branchFrame;
    public Object result;
    public double resultDbl;

    ContinuationJump(NativeContinuation c, CallFrameV2 current) {
        this.capturedFrame = (CallFrameV2) c.getImplementation();
        if (this.capturedFrame == null || current == null) {
            // Continuation and current execution does not share
            // any frames if there is nothing to capture or
            // if there is no currently executed frames
            this.branchFrame = null;
        } else {
            // Search for branch frame where parent frame chains starting
            // from captured and current meet.
            CallFrameV2 chain1 = this.capturedFrame;
            CallFrameV2 chain2 = current;

            // First work parents of chain1 or chain2 until the same
            // frame depth.
            int diff = chain1.frameIndex - chain2.frameIndex;
            if (diff != 0) {
                if (diff < 0) {
                    // swap to make sure that
                    // chain1.frameIndex > chain2.frameIndex and diff > 0
                    chain1 = current;
                    chain2 = this.capturedFrame;
                    diff = -diff;
                }
                do {
                    chain1 = chain1.parentFrame;
                } while (--diff != 0);
                if (chain1.frameIndex != chain2.frameIndex) Kit.codeBug();
            }

            // Now walk parents in parallel until a shared frame is found
            // or until the root is reached.
            while (!Objects.equals(chain1, chain2) && chain1 != null) {
                chain1 = chain1.parentFrame;
                chain2 = chain2.parentFrame;
            }

            this.branchFrame = chain1;
            if (this.branchFrame != null && !this.branchFrame.frozen) Kit.codeBug();
        }
    }
}
