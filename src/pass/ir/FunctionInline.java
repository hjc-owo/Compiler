package pass.ir;

import ir.IRModule;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.Type;
import ir.types.VoidType;
import ir.values.*;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.PhiInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.BrInst;
import ir.values.instructions.terminator.CallInst;
import ir.values.instructions.terminator.RetInst;
import pass.Pass;
import utils.INode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FunctionInline implements Pass.IRPass {
    private IRModule irModule;
    private boolean change = true;

    @Override
    public String getName() {
        return "FunctionInline";
    }

    @Override
    public void run(IRModule m) {
        this.irModule = m;
        while (change) {
            change = false;
            for (INode<Function, IRModule> funNode : irModule.getFunctions()) {
                if (!funNode.getValue().isSuccessorsNotAllLibrary()
                        && !funNode.getValue().isLibraryFunction()
                        && (funNode.getValue().getList().getSize() <= 5)
                ) {
                    inlineFunc(funNode.getValue());
                }
            }

        }
    }

    private void inlineFunc(Function function) {
        if (function.getPredecessors().isEmpty()) {
            return;
        }
        change = true;
        List<CallInst> inlineInst = new ArrayList<>();
        for (Function predecessor : new HashSet<>(function.getPredecessors())) {

            for (INode<ir.values.BasicBlock, Function> basicBlockINode : predecessor.getList()) {
                BasicBlock basicBlock = basicBlockINode.getValue();

                for (INode<Instruction, BasicBlock> instructionINode : basicBlock.getInstructions()) {
                    Instruction instruction = instructionINode.getValue();
                    if (instruction instanceof CallInst) {
                        if (instruction.getOperands().get(0).getName().equals(function.getName())) {
                            inlineInst.add((CallInst) instruction);
                        }
                    }
                }

            }

        }
        for (CallInst inst : inlineInst) {
            inlineCallInst(inst);
        }
        function.getPredecessors().clear();
    }

    private void inlineCallInst(CallInst callInst) {
        BasicBlock oriBasicBlock = callInst.getParent();
        Function oriFunction = oriBasicBlock.getParent();
        BasicBlock insertBasicBlock = new BasicBlock(oriFunction);
        insertBasicBlock.getNode().removeFromList();
        insertBasicBlock.getNode().insertAfter(oriBasicBlock.getNode());

        Function calledFunction = callInst.getCalledFunction();
        Function tmpInlineFunction = new Function(calledFunction.getName(), calledFunction.getType(), false);
        tmpInlineFunction.copyAllFrom(calledFunction);


        INode<Instruction, BasicBlock> next = callInst.getNode().getNext();

        while (next != null) {
            INode<Instruction, BasicBlock> tmp = next.getNext();
            Instruction instruction = next.getValue();
            instruction.getNode().removeFromList();
            instruction.getNode().insertAtEnd(insertBasicBlock.getInstructions());
            next = tmp;
        }
        callInst.getNode().removeFromList();
        callInst.removeUseFromOperands();
        calledFunction.getPredecessors().remove(oriFunction);
        oriFunction.getSuccessors().remove(calledFunction);

        BuildFactory.getInstance().buildBr(oriBasicBlock, tmpInlineFunction.getList().getBegin().getValue());

        for (BasicBlock suc : oriBasicBlock.getSuccessors()) {
            if (suc.equals(tmpInlineFunction.getList().getBegin().getValue())) {
                continue;
            }
            insertBasicBlock.addSuccessor(suc);
            for (int i = 0; i < suc.getPredecessors().size(); i++) {
                if (suc.getPredecessors().get(i).equals(oriBasicBlock)) {
                    suc.getPredecessors().set(i, insertBasicBlock);
                }
            }
        }
        oriBasicBlock.getSuccessors().removeIf(e -> (!e.equals(tmpInlineFunction.getList().getBegin().getValue())));
        // 将调用函数的形式参数换为为传入参数
        List<Value> formalParameters = tmpInlineFunction.getArguments();
        List<Value> actualParameters = new ArrayList<>();
        for (int i = 1; i < callInst.getOperands().size(); i++) {
            actualParameters.add(callInst.getOperands().get(i));
        }

        for (int i = 0; i < formalParameters.size(); i++) {
            Value fP = formalParameters.get(i);
            Value aP = actualParameters.get(i);
            if (aP.getType() instanceof IntegerType) {
                fP.replaceUsedWith(aP);
            } else {
                for (Use v : fP.getUsesList()) {
                    if (!(v.getUser() instanceof Instruction)) {
                        continue;
                    }
                    Instruction instruction = (Instruction) v.getUser();
                    if (instruction.getOperator().equals(Operator.Store)) {
                        AllocaInst allocaInst = (AllocaInst) ((StoreInst) instruction).getPointer();
                        for (Use use : allocaInst.getUsesList()) {
                            if (!(use.getUser() instanceof Instruction)) {
                                continue;
                            }
                            Instruction instruction1 = (Instruction) use.getUser();
                            if (instruction1.getOperator().equals(Operator.Load)) {
                                instruction1.replaceUsedWith(aP);
                            }
                        }
                    }

                }
            }

        }
        ArrayList<Instruction> rets = new ArrayList<>();
        ArrayList<Instruction> calls = new ArrayList<>();
        ArrayList<BasicBlock> moveBB = new ArrayList<>();
        for (INode<BasicBlock, Function> basicBlockINode : tmpInlineFunction.getList()) {
            BasicBlock basicBlock = basicBlockINode.getValue();
            moveBB.add(basicBlock);
            for (INode<Instruction, BasicBlock> InstNode : basicBlock.getInstructions()) {
                Instruction instruction = InstNode.getValue();
                if (instruction instanceof RetInst) {
                    rets.add(instruction);
                } else if (instruction instanceof CallInst) {
                    calls.add(instruction);
                }
            }
        }
        Type retType = ((FunctionType) calledFunction.getType()).getReturnType();
        if (retType instanceof IntegerType) {
            PhiInst phi = new PhiInst(insertBasicBlock, ((FunctionType) calledFunction.getType()).getReturnType(), new ArrayList<>());
            callInst.replaceUsedWith(phi);
            for (Instruction instruction : rets) {
                phi.addOperand(instruction.getOperands().get(0));
                instruction.removeUseFromOperands();
                BrInst brInst = new BrInst(instruction.getParent(), insertBasicBlock);
                brInst.getNode().insertBefore(instruction.getNode());
                insertBasicBlock.addPredecessor(instruction.getParent());
                instruction.getParent().addSuccessor(insertBasicBlock);
                instruction.getNode().removeFromList();
            }
            phi.getNode().insertAtBegin(insertBasicBlock.getInstructions());
        } else if (retType instanceof VoidType) {
            for (Instruction instruction : rets) {
                insertBasicBlock.addPredecessor(instruction.getParent());
                instruction.getParent().addSuccessor(insertBasicBlock);
                BrInst brInst = new BrInst(instruction.getParent(), insertBasicBlock);
                brInst.getNode().insertBefore(instruction.getNode());
                instruction.getNode().removeFromList();
            }
        }

        for (BasicBlock basicBlock : moveBB) {
            basicBlock.getNode().removeFromList();
            basicBlock.getNode().insertBefore(insertBasicBlock.getNode());

        }
        for (Instruction call : calls) {
            Function calledFunc = ((CallInst) call).getCalledFunction();
            calledFunc.addPredecessor(oriFunction);
            oriFunction.addSuccessor(calledFunc);
        }
        tmpInlineFunction.getNode().removeFromList();
        IRModule.getInstance().refreshRegNumber();
    }
}
