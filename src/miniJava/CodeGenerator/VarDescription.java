package miniJava.CodeGenerator;

public class VarDescription extends RuntimeDescription {
    // Offset from SB if static, and from OB or LB if not static.
    public int offset;

    public VarDescription() {
        super();
    }

    public VarDescription(int size) {
        super(size);
    }
}
