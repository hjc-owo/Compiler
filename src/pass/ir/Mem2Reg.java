package pass.ir;

import ir.IRModule;
import ir.values.Function;
import pass.Pass;
import utils.INode;

public class Mem2Reg implements Pass.IRPass {

    @Override
    public String getName() {
        return "Mem2Reg";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> f : m.getFunctions()) {
            if (!f.getValue().isLibraryFunction()) {
                mem2reg(f.getValue());
            }
        }
    }

    private void mem2reg(Function f) {
        // TODO
    }
}
