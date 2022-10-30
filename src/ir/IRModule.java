package ir;

import ir.values.*;
import ir.values.instructions.Instruction;
import utils.IList;
import utils.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IRModule {
    private static final IRModule module = new IRModule();
    private List<GlobalVar> globalVars;
    private IList<Function, IRModule> functions;
    private HashMap<Integer, Instruction> instructions;

    private IRModule() {
        this.globalVars = new ArrayList<>();
        this.functions = new IList<>(this);
        this.instructions = new HashMap<>();
    }

    public static IRModule getInstance() {
        return module;
    }

    public void addInstruction(int handle, Instruction instruction) {
        this.instructions.put(handle, instruction);
    }

    public IList<Function, IRModule> getFunctions() {
        return this.functions;
    }

    public void addGlobalVar(GlobalVar globalVariable) {
        this.globalVars.add(globalVariable);
    }

    public List<GlobalVar> getGlobalVars() {
        return globalVars;
    }

    public void refreshRegNumber() {
        for (INode<Function, IRModule> function : functions) {
            Value.REG_NUMBER = 0;
            function.getValue().refreshArgReg();
            if (!function.getValue().isLibraryFunction()) {
                for (INode<BasicBlock, Function> basicBlock : function.getValue().getList()) {
                    if (basicBlock.getValue().getInstructions().isEmpty()) {
                        BuildFactory.getInstance().checkBlockEnd(basicBlock.getValue());
                    }
                    basicBlock.getValue().setName(";<label>:" + Value.REG_NUMBER++);
                    basicBlock.getValue().refreshReg();
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (GlobalVar globalVar : globalVars) {
            s.append(globalVar.toString()).append("\n");
        }
        s.append("\n");
        refreshRegNumber();
        for (INode<Function, IRModule> function : functions) {
            if (function.getValue().isLibraryFunction()) {
                s.append("declare ").append(function.getValue().toString()).append("\n\n");
            } else {
                s.append("define dso_local ").append(function.getValue().toString()).append("{\n");
                for (INode<BasicBlock, Function> basicBlock : function.getValue().getList()) {
                    if (basicBlock != function.getValue().getList().getBegin()) {
                        s.append("\n");
                    }
                    s.append(basicBlock.getValue().getName()).append(":\n").append(basicBlock.getValue().toString()).append("}\n\n");
                }
            }
        }
        return s.toString();
    }
}
