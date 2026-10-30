package rip.ysm.api.env;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注「只在某一侧存在/可调用」的类或成员（替代 Fabric API 的
 * {@code net.fabricmc.api.Environment}）。
 *
 * <p><b>本注解没有任何运行时效果</b>——它只是给人和静态分析看的契约标记，
 * 与 Fabric 版行为一致（fabric-loader 也不会据此剥离代码）。真正的单侧安全
 * 依靠调用方在入口处按 {@code PlatformAPI.isServer()} 分支，或依赖各加载器
 * 只在对应物理端注册事件。
 *
 * <p>之所以用项目自己的注解而不是继续引用 fabric-api：多加载器下 common
 * 不应把 fabric-loader 作为编译依赖。
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface Environment {

    EnvType value();
}
