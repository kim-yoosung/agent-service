package com.example.agent;

import com.example.tracing.apitracing.DispatcherServletAdvice;
import com.example.tracing.dbtracing.PrepareStatementExecuteAdvice;
import com.example.tracing.logging.DynamicLogFileGenerator;
import com.example.tracing.outgingtracing.RestTemplateInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;

import java.lang.instrument.Instrumentation;
import java.net.URI;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] Starting...");
        DynamicLogFileGenerator.initLogger();

        AgentBuilder.Listener listener = new AgentBuilder.Listener() {
            @Override
            public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                System.out.println("[Agent] Discovered: " + typeName);
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                System.out.println("[Agent] Transformed: " + typeDescription.getName());
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
                System.out.println("[Agent] Ignored: " + typeDescription.getName());
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                System.err.println("[Agent] Error transforming: " + typeName);
                throwable.printStackTrace();
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                System.out.println("[Agent] Completed: " + typeName);
            }
        };

        // DispatcherServlet 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(listener)
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.hasSuperType(named("org.springframework.web.servlet.DispatcherServlet")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doDispatch")
                                .and(ElementMatchers.takesArguments(2))
                                .and(ElementMatchers.takesArgument(0, named("javax.servlet.http.HttpServletRequest")))
                                .and(ElementMatchers.takesArgument(1, named("javax.servlet.http.HttpServletResponse"))))
                                .intercept(MethodDelegation.to(DispatcherServletAdvice.class))
                )
                .installOn(inst);

        // OutgoingHttp 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(listener)
                .ignore(none())
                .type(hasSuperType(named("org.springframework.web.client.RestTemplate")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("doExecute")
                                .and(takesArguments(3))
                                .and(takesArgument(0, URI.class))
                                .and(takesArgument(1, HttpMethod.class))
                                .and(takesArgument(2, HttpEntity.class)))
                                .intercept(MethodDelegation.to(RestTemplateInterceptor.class))
                )
                .installOn(inst);

        // DB Connection 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(listener)
                .ignore(none())
                .type(hasSuperType(named("java.sql.PreparedStatement")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("execute")
                                .or(named("executeQuery"))
                                .or(named("executeUpdate")))
                                .intercept(MethodDelegation.to(PrepareStatementExecuteAdvice.class))
                )
                .installOn(inst);

        System.out.println("[Agent] Started successfully");
    }
}
