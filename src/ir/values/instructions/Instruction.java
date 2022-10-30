package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.User;
import ir.values.instructions.terminator.BrInst;
import ir.values.instructions.terminator.RetInst;
import utils.INode;

public class Instruction extends User {
    private Operator op;
    private INode<Instruction, BasicBlock> node;
    private int handler;
    private static int HANDLER = 0;

    public Instruction(Type type, Operator op, BasicBlock basicBlock) {
        super("", type);
        this.op = op;
        this.node = new INode<>(this);
        this.handler = HANDLER++;
        this.getModule().addInstruction(handler, this);
    }

    public Operator getOperator() {
        return op;
    }

    public void setOperator(Operator op) {
        this.op = op;
    }

    public INode<Instruction, BasicBlock> getNode() {
        return node;
    }

    public void setNode(INode<Instruction, BasicBlock> node) {
        this.node = node;
    }

    public int getHandler() {
        return handler;
    }

    public void setHandler(int handler) {
        this.handler = handler;
    }

    public static int getHANDLER() {
        return HANDLER;
    }

    public static void setHANDLER(int HANDLER) {
        Instruction.HANDLER = HANDLER;
    }

    public BasicBlock getParent() {
        return this.getNode().getParent().getValue();
    }

    public void addInstToBlock(BasicBlock basicBlock) {
        if (basicBlock.getInstructions().getEnd() == null ||
                (!(basicBlock.getInstructions().getEnd().getValue() instanceof BrInst) &&
                        !(basicBlock.getInstructions().getEnd().getValue() instanceof RetInst))) {
            this.getNode().insertAtEnd(basicBlock.getInstructions());
        } else {
            this.removeUseFromOperands();
        }
    }

    public void addInstToBlockBegin(BasicBlock basicBlock) {
        this.getNode().insertAtBegin(basicBlock.getInstructions());
    }
}
