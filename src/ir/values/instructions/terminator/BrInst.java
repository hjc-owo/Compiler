package ir.values.instructions.terminator;

import ir.types.IntegerType;
import ir.types.VoidType;
import ir.values.BasicBlock;
import ir.values.BuildFactory;
import ir.values.ConstInt;
import ir.values.Value;
import ir.values.instructions.Operator;

public class BrInst extends TerminatorInst {
    public BrInst(BasicBlock basicBlock, BasicBlock trueBlock) {
        super(VoidType.voidType, Operator.Br, basicBlock);
        this.addOperand(trueBlock);
        // 添加前驱后继
        if (basicBlock != null) {
            if (!(basicBlock.getInstructions().getEnd().getValue() instanceof BrInst) &&
                    !(basicBlock.getInstructions().getEnd().getValue() instanceof RetInst)) {
                basicBlock.addSuccessor(trueBlock);
                trueBlock.addSuccessor(basicBlock);
            }
        }
    }

    public BrInst(BasicBlock basicBlock, Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
        super(VoidType.voidType, Operator.Br, basicBlock);
        // conversion handler
        Value condTmp = cond;
        if (!(cond.getType() instanceof IntegerType && ((IntegerType) cond.getType()).isI1())) {
            condTmp = BuildFactory.getInstance().buildBinary(basicBlock, Operator.Ne, cond, new ConstInt(0));
        }
        this.addOperand(condTmp);
        this.addOperand(trueBlock);
        this.addOperand(falseBlock);
        // 添加前驱后继
        if (basicBlock.getInstructions().getEnd() == null ||
                (!(basicBlock.getInstructions().getEnd().getValue() instanceof BrInst) &&
                        !(basicBlock.getInstructions().getEnd().getValue() instanceof RetInst))) {
            basicBlock.addSuccessor(trueBlock);
            basicBlock.addSuccessor(falseBlock);
            trueBlock.addSuccessor(basicBlock);
            falseBlock.addSuccessor(basicBlock);
        }
    }

    @Override
    public String toString() {
        if (this.getOperands().size() == 1) {
            return "br label %" + this.getOperands().get(0).getName();
        } else {
            return "br " + this.getOperands().get(0).toString() + ", label %" + this.getOperands().get(1).getName() + ", label %" + this.getOperands().get(2).getName();
        }
    }
}

