package com.github.mcollovati.vertx.utils;

import java.util.concurrent.Callable;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ByteBuddyTest {


    @Test
    public void testRedefineCtor() {
        redefineMyClassCtor();
        Dependency d = new Dependency("grp", "art", "12");
        VersionHolder vh = new VersionHolder();
        MyClass m = new MyClass(d, vh);
        assertThat(vh.version).isEqualTo(d.version);
    }
    @Test
    public void testRedefineMethod() {
        redefineMyClassMetod();
        Dependency d = new Dependency("grp", "art", "12");
        VersionHolder vh = new VersionHolder();
        MyClass m = new MyClass(d, vh);
        m.register(d);
        
    }


    private void redefineMyClassMetod() {
        ByteBuddyAgent.install();
        new ByteBuddy().rebase(MyClass.class)
            .constructor(ElementMatchers.takesArguments(Dependency.class, VersionHolder.class))
            //.defineMethod("getVersion", String.class, Visibility.PUBLIC)
            .intercept(
                SuperMethodCall.INSTANCE
                    .andThen(
                        MethodDelegation.to(LoggerInterceptor2.class))
            )
            .make()
            .load(MyClass.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    private void redefineMyClassCtor() {
        ByteBuddyAgent.install();
        TypeDescription typeDef = TypePool.Default.ofClassPath()
            .describe("com.github.mcollovati.vertx.utils.ByteBuddyTest$MyClass").resolve();
        new ByteBuddy().rebase(typeDef, ClassFileLocator.ForClassLoader.ofClassPath())
            .method(ElementMatchers.named("register"))
            //.defineMethod("getVersion", String.class, Visibility.PUBLIC)
            .intercept(
                MethodDelegation.to(new Object() {
                    void delegete(@SuperCall Callable<?> zuper) throws Exception {
                        System.out.println("BEFORE");
                        zuper.call();
                        System.out.println("fter");
                    }
                })
                    .andThen(
                        SuperMethodCall.INSTANCE)
            )
            .make()
            .load(Thread.currentThread().getContextClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    static class LoggerInterceptor2 {

        public static void delegate(@This Object zuper, @AllArguments Object... args) throws Exception {
            Dependency dep = (Dependency) args[0];
            VersionHolder vh = (VersionHolder) args[1];
            vh.setVersion(dep.version);
        }
    }

    public static class MyClass {

        private final String groupId;
        private final String artifactId;

        public MyClass(Dependency d, VersionHolder v) {
            this.groupId = d.groupId;
            this.artifactId = d.artifactId;
        }

        synchronized void register(Dependency dependency) {
            System.out.println("=========== ORI");
        }
    }

    public static class VersionHolder {
        private String version;

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class Dependency {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public Dependency(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getVersion() {
            return version;
        }
    }
}
