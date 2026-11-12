package org.jmouse.web.mvc;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.result.HandlerResult;
import org.jmouse.web.mvc.result.ReturnValueHandler;
import org.jmouse.web.mvc.result.ReturnValueProcessor;
import org.junit.jupiter.api.Test;

class HandlerExecutionContractTest {

    @Test
    void postHandleRunsBeforeRenderingAndCanUpdateTheRequestModel() throws Exception {
        List<String> events = new ArrayList<>();
        Handler chain = new Handler(() -> new Object());
        chain.addInterceptor(
            new HandlerInterceptor() {
                @Override
                public HandlerResult postHandle(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler,
                    HandlerResult result
                ) {
                    events.add("postHandle");
                    RequestModel.get(request).addAttribute("completed", true);
                    return HandlerResult.returned(
                        "intercepted",
                        ((HandlerResult.ReturnValue) result).returnType()
                    );
                }
            }
        );
        MethodParameter type = returnType("text");
        HttpServletRequest request = request();
        HandlerDispatcher dispatcher = dispatcher(
            chain,
            context -> {
                events.add("invoke");
                return HandlerResult.returned("body", type);
            },
            List.of(),
            result -> {
                assertEquals(true, RequestModel.get(request).getAttribute("completed"));
                assertEquals("intercepted", result.value());
                events.add("render");
            }
        );
        dispatcher.dispatch(request, response());
        assertEquals(List.of("invoke", "postHandle", "render"), events);
    }

    @Test
    void directHttpHandlersNeverEnterReturnValueRendering() {
        Handler chain = new Handler(() -> new Object());
        List<String> events = new ArrayList<>();
        HandlerDispatcher dispatcher = dispatcher(
            chain,
            context -> {
                events.add("direct");
                return HandlerResult.handled();
            },
            List.of(),
            result -> fail("A handled response must not be rendered again")
        );
        dispatcher.dispatch(request(), response());
        assertEquals(List.of("direct"), events);
    }

    @Test
    void resolvedExceptionsKeepTheirOwnDeclaredTypeAndUseTheSharedRenderer() throws Exception {
        MethodParameter originalType = returnType("number");
        MethodParameter errorType = returnType("text");
        Handler chain = new Handler(
            new MappedHandler() {
                @Override
                public Object handler() {
                    return this;
                }

                @Override
                public MethodParameter returnParameter() {
                    return originalType;
                }
            }
        );
        List<String> events = new ArrayList<>();
        ExceptionResolver resolver = new ExceptionResolver() {
            @Override
            public boolean supportsException(Throwable exception) {
                return true;
            }

            @Override
            public Optional<HandlerResult> resolveException(
                RequestContext context,
                MappedHandler handler,
                Exception exception
            ) {
                assertEquals("failure", exception.getMessage());
                return Optional.of(HandlerResult.returned("recovered", errorType));
            }
        };
        HandlerDispatcher dispatcher = dispatcher(
            chain,
            context -> {
                throw new IllegalArgumentException("failure");
            },
            List.of(resolver),
            result -> {
                assertEquals(String.class, result.returnType().getReturnType());
                events.add((String) result.value());
            }
        );
        dispatcher.dispatch(request(), response());
        assertEquals(List.of("recovered"), events);
    }

    @Test
    void modelIsSharedWithinOneRequestAndIsolatedBetweenRequests() {
        HttpServletRequest first = request();
        HttpServletRequest second = request();
        RequestModel.get(first).addAttribute("name", "first");
        assertSame(RequestModel.get(first), RequestModel.get(first));
        assertEquals("first", RequestModel.get(first).getAttribute("name"));
        assertNull(RequestModel.get(second).getAttribute("name"));
        assertNotSame(RequestModel.get(first), RequestModel.get(second));
    }

    @Test
    void nullMethodReturnRemainsTypedAndIsNotConfusedWithHandledResponse() throws Exception {
        List<Object> values = new ArrayList<>();
        ReturnValueProcessor processor = new ReturnValueProcessor(
            List.of(
                renderer(result -> {
                    assertEquals(String.class, result.returnType().getReturnType());
                    values.add(result.value());
                })
            )
        );
        RequestContext context = new RequestContext(request(), response());
        processor.process(HandlerResult.returned(null, returnType("text")), context);
        processor.process(HandlerResult.handled(), context);
        assertEquals(1, values.size());
        assertNull(values.getFirst());
    }

    private static HandlerDispatcher dispatcher(
        Handler chain,
        Function<RequestContext, HandlerResult> invocation,
        List<ExceptionResolver> resolvers,
        Consumer<HandlerResult.ReturnValue> render
    ) {
        HandlerMapping mapping = new HandlerMapping() {
            @Override
            public Handler getHandler(HttpServletRequest request) {
                return chain;
            }

            @Override
            public boolean supportsMappedHandler(Object handler) {
                return true;
            }
        };
        HandlerAdapter adapter = new HandlerAdapter() {
            @Override
            public boolean supportsHandler(MappedHandler handler) {
                return true;
            }

            @Override
            public HandlerResult handle(RequestContext context, MappedHandler handler) {
                return invocation.apply(context);
            }
        };
        return new HandlerDispatcher(
            List.of(mapping),
            List.of(adapter),
            resolvers,
            new ReturnValueProcessor(List.of(renderer(render)))
        );
    }

    private static ReturnValueHandler renderer(Consumer<HandlerResult.ReturnValue> render) {
        return new ReturnValueHandler() {
            @Override
            public boolean supportsReturnType(HandlerResult.ReturnValue result) {
                return true;
            }

            @Override
            public void handleReturnValue(HandlerResult.ReturnValue result, RequestContext context) {
                render.accept(result);
            }
        };
    }

    private static MethodParameter returnType(String method) throws NoSuchMethodException {
        return MethodParameter.forMethod(Controller.class.getDeclaredMethod(method), -1);
    }

    private static HttpServletRequest request() {
        Map<String, Object> attributes = new HashMap<>();
        return (HttpServletRequest) Proxy.newProxyInstance(
            HandlerExecutionContractTest.class.getClassLoader(),
            new Class<?>[] { HttpServletRequest.class },
            (proxy, method, arguments) ->
                switch (method.getName()) {
                    case "getAttribute" -> attributes.get(arguments[0]);
                    case "setAttribute" -> {
                        attributes.put((String) arguments[0], arguments[1]);
                        yield null;
                    }
                    case "removeAttribute" -> attributes.remove(arguments[0]);
                    case "getMethod" -> "GET";
                    default -> method.getReturnType() == boolean.class
                        ? false
                        : method.getReturnType() == int.class
                          ? 0
                          : null;
                }
        );
    }

    private static HttpServletResponse response() {
        return (HttpServletResponse) Proxy.newProxyInstance(
            HandlerExecutionContractTest.class.getClassLoader(),
            new Class<?>[] { HttpServletResponse.class },
            (proxy, method, arguments) ->
                method.getReturnType() == boolean.class
                    ? false
                    : method.getReturnType() == int.class
                      ? 0
                      : null
        );
    }

    static final class Controller {

        public String text() {
            return "value";
        }

        public Integer number() {
            return 1;
        }
    }
}
