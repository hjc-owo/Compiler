package pass;

import config.Config;
import ir.IRModule;
import pass.ir.DeadCodeElimination;

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
    }

    public void runIRPasses() {
        irPasses.forEach(p -> p.run(IRModule.getInstance()));
    }
}
