package miniJava.CodeGenerator;

import miniJava.mJAM.Machine;

public class MethodDescription extends RuntimeDescription {
    // Offset of the method's code from CB.
    int cbOffset;
    // Total size of all arguments of the method.
    int argSize;

    public MethodDescription(int cbOffset) {
        this.cbOffset = cbOffset;
        // argSize is updated as code for it's arguments is generated.
        argSize = 0;
        // Even without any code, the method takes up space after LB to store the link data.
        size = Machine.linkDataSize;
    }
}
