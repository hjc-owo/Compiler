package pass.ir;

import ir.IRModule;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.CallInst;
import ir.values.instructions.terminator.RetInst;
import pass.Pass;
import utils.INode;

import java.util.*;

public class GlobalVarLocalize implements Pass.IRPass {

    private final Map<GlobalVar, Set<Function>> userFuncMap = new HashMap<>();
    private final Map<Function, Set<GlobalVar>> useGv = new HashMap<>();
    private Set<Function> visit = new HashSet<>();
    private List<Function> line = new ArrayList<>();
    private boolean flag = true;
    private Set<Function> recurFunction = new HashSet<>();


    @Override
    public String getName() {
        return "GlobalVarLocalize";
    }

    @Override
    public void run(IRModule m) {
        // 去除无人调用函数 并且解除use关系
        removeNotUseFunc(m);
        // 得到每个全局变量的使用者
        for (GlobalVar gv : m.getGlobalVars()) {
            userFuncMap.put(gv, new HashSet<>());
            for (Use use : gv.getUsesList()) {
                userFuncMap.get(gv).add(((Instruction) use.getUser()).getParent().getParent());
            }
        }
        // 找到每个函数使用的全局变量
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function function = funcEntry.getValue();
            Set<GlobalVar> list = new HashSet<>();
            for (GlobalVar gv : m.getGlobalVars()) {
                visit.clear();
                if (checkHahGv(function, gv)) {
                    list.add(gv);
                }
            }
            useGv.put(function, list);
        }
        // 找到递归函数
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function func = funcEntry.getValue();
            if (func.isLibraryFunction()) {
                continue;
            }
            if (judgeRecur(func)) {
                recurFunction.add(func);
            }
        }
        visit = new HashSet<>();
        for (Function i : recurFunction) {
            line = new ArrayList<>();
            bfs(i);
        }
        recurFunction = visit;
        //change it
        change(m);
    }

    private void removeNotUseFunc(IRModule m) {
        Set<INode<Function, IRModule>> notUseFunc = new HashSet<>();
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function func = funcEntry.getValue();
            if (!func.getName().equals("main") && func.getPredecessors().isEmpty()) {
                notUseFunc.add(funcEntry);
            }
        }
        for (INode<Function, IRModule> funcEntry : notUseFunc) {
            for (GlobalVar globalVar : m.getGlobalVars()) {
                List<Use> removeList = new ArrayList<>();
                for (Use use : globalVar.getUsesList()) {
                    if (((Instruction) use.getUser()).getParent().getParent().equals(funcEntry.getValue())) {
                        removeList.add(use);
                    }
                }
                for (Use u : removeList) {
                    globalVar.getUsesList().remove(u);
                }
            }
        }
        for (INode<Function, IRModule> funcEntry : notUseFunc) {
            funcEntry.removeFromList();
        }
    }

    public boolean checkHahGv(Function function, GlobalVar gv) {
        if (visit.contains(function)) {
            return false;
        }
        visit.add(function);
        if (userFuncMap.get(gv).contains(function)) {
            return true;
        }
        for (Function i : function.getSuccessors()) {
            if (checkHahGv(i, gv)) {
                return true;
            }
        }
        return false;
    }

    private boolean judgeRecur(Function function) {
        visit.clear();
        flag = true;
        dfs(function);
        return visit.contains(function);
    }

    private void dfs(Function function) {
        if (visit.contains(function)) {
            return;
        }
        if (flag) {
            flag = false;
        } else {
            visit.add(function);
        }
        for (Function f : function.getSuccessors()) {
            dfs(f);
        }
    }

    private void bfs(Function func) {
        if (visit.contains(func)) {
            return;
        }
        int top = 0;
        int end = 0;
        top++;
        line.add(func);
        visit.add(func);
        while (end < top) {
            for (Function f : line.get(end).getSuccessors()) {
                if (!visit.contains(f)) {
                    top++;
                    line.add(f);
                    visit.add(f);
                }
            }
            end++;
        }
    }

    private void change(IRModule m) {
        for (GlobalVar gv : m.getGlobalVars()) {
            if (((PointerType) gv.getType()).getTargetType() instanceof ArrayType) {
                continue;
            } else if (gv.isConst()) {
                continue;
            }
            Set<Function> userFunc = userFuncMap.get(gv);
            if (!userFunc.isEmpty()) {
                Function keep = null;
                boolean flag = userFunc.size() == 1;
                if (flag) {
                    for (Function f : userFunc) {
                        keep = f;
                        if (!f.getName().equals("main")) {
                            flag = false;
                        }
                    }
                }

                if (flag) {
                    INode<BasicBlock, Function> head = keep.getList().getBegin();
                    AllocaInst pointer = new AllocaInst(head.getValue(), false, ((PointerType) gv.getType()).getTargetType());
                    pointer.getNode().insertAtBegin(head.getValue().getInstructions());
                    if (gv.getValue() != null) {
                        StoreInst store = new StoreInst(head.getValue(), pointer, gv.getValue());
                        store.getNode().insertAfter(pointer.getNode());
                    }
                    gv.replaceUsedWith(pointer);
                } else {
                    for (Function function : userFunc) {
                        changeAtSingleFunc(gv, function);
                    }
                }
            }
        }
    }

    private void changeAtSingleFunc(GlobalVar gv, Function func) {
        BasicBlock head = func.getList().getBegin().getValue();
        AllocaInst pointer = new AllocaInst(head, false, ((PointerType) gv.getType()).getTargetType());
        ArrayList<Use> delList = new ArrayList<>();
        pointer.getNode().insertAtBegin(head.getInstructions());
        for (Use u : gv.getUsesList()) {
            if (((Instruction) u.getUser()).getParent().getParent().equals(func)) {
                delList.add(u);
            }
        }
        for (Use use : delList) {
            User user = use.getUser();
            int rank = use.getPosOfOperand();
            Value value = use.getValue();
            user.getOperands().set(rank, null);
            value.removeFromUseList(use);
            user.setOperands(rank, pointer);
        }
        LoadInst loadValue = new LoadInst(head, gv);
        loadValue.getNode().insertAfter(pointer.getNode());
        StoreInst store = new StoreInst(head, pointer, loadValue);
        store.getNode().insertAfter(loadValue.getNode());
        for (INode<BasicBlock, Function> bbEntry : func.getList()) {
            BasicBlock basicBlock = bbEntry.getValue();
            for (INode<Instruction, BasicBlock> instrEntry : basicBlock.getInstructions()) {
                Instruction instr = instrEntry.getValue();
                if (instr instanceof CallInst) {
                    if (useGv.get(((CallInst) instr).getCalledFunction()).contains(gv)) {
                        LoadInst load1 = new LoadInst(basicBlock, pointer);
                        load1.getNode().insertBefore(instr.getNode());
                        StoreInst store1 = new StoreInst(basicBlock, gv, load1);
                        store1.getNode().insertBefore(instr.getNode());
                        LoadInst load2 = new LoadInst(basicBlock, gv);
                        load2.getNode().insertAfter(instr.getNode());
                        StoreInst store2 = new StoreInst(basicBlock, pointer, load2);
                        store2.getNode().insertAfter(load2.getNode());
                    }
                }
                if (instr instanceof RetInst) {
                    LoadInst load1 = new LoadInst(basicBlock, pointer);
                    load1.getNode().insertBefore(instr.getNode());
                    StoreInst store1 = new StoreInst(basicBlock, gv, load1);
                    store1.getNode().insertAfter(load1.getNode());
                }
            }
        }
    }
}

