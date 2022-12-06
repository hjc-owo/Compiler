package ir.values.instructions.terminator;

import ir.types.*;
import ir.values.BasicBlock;
import ir.values.BuildFactory;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Operator;

import java.util.List;

public class CallInst extends TerminatorInst {
    public CallInst(BasicBlock basicBlock, Function function, List<Value> args) {
        super(((FunctionType) function.getType()).getReturnType(), Operator.Call, basicBlock);
        if (!(((FunctionType) function.getType()).getReturnType() instanceof VoidType)) {
            this.setName("%" + REG_NUMBER++);
        }
        this.addOperand(function);

        for (int i = 0; i < args.size(); i++) {
            Type curType = args.get(i).getType();
            Type realType = ((FunctionType) function.getType()).getParametersType().get(i);
            Value tmp = convType(args.get(i), basicBlock, curType, realType);
            this.addOperand(tmp);
        }

        Function curFunction = basicBlock.getNode().getParent().getValue();
        function.addPredecessor(curFunction);
        curFunction.addSuccessor(function);
    }

    private Value convType(Value value, BasicBlock basicBlock, Type curType, Type realType) {
        boolean isCurI1 = curType instanceof IntegerType && ((IntegerType) curType).isI1();
        boolean isCurI32 = curType instanceof IntegerType && ((IntegerType) curType).isI32();
        boolean isRealI1 = realType instanceof IntegerType && ((IntegerType) realType).isI1();
        boolean isRealI32 = realType instanceof IntegerType && ((IntegerType) realType).isI32();
        if (!isCurI1 && !isCurI32 && !isRealI1 && !isRealI32) {
            return value;
        } else if ((isCurI1 && isRealI1) || (isCurI32 && isRealI32)) {
            return value;
        } else if (isCurI1 && isRealI32) {
            return BuildFactory.getInstance().buildZext(value, basicBlock);
        } else if (isCurI32 && isRealI1) {
            return BuildFactory.getInstance().buildConvToI1(value, basicBlock);
        } else {
            return value;
        }
    }

    public Function getCalledFunction() {
        return (Function) this.getOperands().get(0);
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Type returnType = ((FunctionType) this.getCalledFunction().getType()).getReturnType();
        if (returnType instanceof VoidType) {
            s.append("call ");
        } else {
            s.append(this.getName()).append(" = call ");
        }
        s.append(returnType.toString()).append(" @").append(this.getCalledFunction().getName()).append("(");
        for (int i = 1; i < this.getOperands().size(); i++) {
            s.append(this.getOperands().get(i).getType().toString()).append(" ").append(this.getOperands().get(i).getName());
            if (i != this.getOperands().size() - 1) {
                s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }
}
