package org.jmouse.vm.executable;

public final class ExecutableNodeNames {

    private static final String NAMESPACE = "vm.executable.";

    public static final String BLOCK = NAMESPACE + "block";
    public static final String IF = NAMESPACE + "if";
    public static final String TRY = NAMESPACE + "try";
    public static final String CATCH = NAMESPACE + "catch";
    public static final String RESOURCE = NAMESPACE + "resource";
    public static final String LOCAL_REFERENCE = NAMESPACE + "local-reference";
    public static final String CONTROL_FLOW = NAMESPACE + "control-flow";
    public static final String BASIC_BLOCK = NAMESPACE + "basic-block";
    public static final String JUMP_TERMINATOR = NAMESPACE + "jump-terminator";
    public static final String BRANCH_TERMINATOR = NAMESPACE + "branch-terminator";
    public static final String RETURN_TERMINATOR = NAMESPACE + "return-terminator";
    public static final String CALL = NAMESPACE + "call";
    public static final String EVALUATION = NAMESPACE + "evaluation";

    public static final String STATEMENTS = "statements";
    public static final String CONDITION = "condition";
    public static final String CONSEQUENT = "consequent";
    public static final String ALTERNATIVE = "alternative";
    public static final String BODY = "body";
    public static final String RESOURCES = "resources";
    public static final String CATCHES = "catches";
    public static final String FINALLY_BODY = "finally-body";
    public static final String INITIALIZER = "initializer";
    public static final String BLOCKS = "blocks";
    public static final String OPERATIONS = "operations";
    public static final String TERMINATOR = "terminator";
    public static final String VALUE = "value";
    public static final String RECEIVER = "receiver";
    public static final String ARGUMENTS = "arguments";

    private ExecutableNodeNames() {}
}
