package pass;

import config.Config;
import ir.IRModule;
import pass.ir.*;

import java.util.ArrayList;
import java.util.List;

public class PassModule {
    private static final PassModule INSTANCE = new PassModule();

    private final List<Pass.IRPass> irPasses = new ArrayList<>();

    public static PassModule getInstance() {
        return INSTANCE;
    }

    public PassModule() {
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.GlobalVarLocalize) {
            irPasses.add(new GlobalVarLocalize());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.Mem2Reg) {
            irPasses.add(new Mem2Reg());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.GVNGCM) {
            irPasses.add(new GVNGCM());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.FunctionInline) {
            irPasses.add(new FunctionInline());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.MarkConstArray) {
            irPasses.add(new MarkConstArray());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.BranchOptimization) {
            irPasses.add(new BranchOptimization());
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
        if (Config.LCSSA) {
            irPasses.add(new LCSSA());
            if (Config.RemoveUnUseLoop) {
                irPasses.add(new RemoveUnUseLoop());
                if (Config.GVNGCM) {
                    irPasses.add(new GVNGCM());
                }
            }
        }
        if (Config.DeadCodeElimination) {
            irPasses.add(new DeadCodeElimination());
        }
    }

    public void runIRPasses() {
        irPasses.forEach(p -> p.run(IRModule.getInstance()));
    }
}
