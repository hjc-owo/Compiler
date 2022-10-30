package ir.values.instructions;

public enum Operator {
    Add, Sub, Mul, Div, Mod, And, Or,  // 二元运算符
    Lt, Le, Ge, Gt, Eq, Ne, // 关系运算符
    Zext, Bitcast, // 类型转换
    Alloca, Load, Store, GEP, // 内存操作
    Phi, MemPhi, LoadDep, // Phi 指令
    Br, Call, Ret, // 跳转指令
    Not // 非运算符
}
