package com.elfmcys.yesstevemodel.molang.runtime;

/** 资源预算耗尽必须中断当前脚本，不能被函数参数的安全求值当作普通空值吞掉。 */
public final class EvaluationLimitException extends RuntimeException {
    public EvaluationLimitException(String message) {
        super(message, null, false, false);
    }
}
