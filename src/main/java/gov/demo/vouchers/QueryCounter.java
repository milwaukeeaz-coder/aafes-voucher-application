package gov.demo.vouchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Demo tooling: counts every JDBC statement the application prepares, so the
 * FinOps act can show queries per request. Not part of the business logic.
 */
@Component
public class QueryCounter implements BeanPostProcessor {
    private static final AtomicLong COUNT = new AtomicLong();

    public static long count() {
        return COUNT.get();
    }

    public static void reset() {
        COUNT.set(0);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource ds) {
            return Proxy.newProxyInstance(bean.getClass().getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> {
                    Object result = call(ds, method, args);
                    if (method.getName().equals("getConnection") && result instanceof Connection c) {
                        return wrap(c);
                    }
                    return result;
                });
        }
        return bean;
    }

    private static Connection wrap(Connection c) {
        return (Connection) Proxy.newProxyInstance(c.getClass().getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                String n = method.getName();
                if (n.equals("prepareStatement") || n.equals("createStatement") || n.equals("prepareCall")) {
                    COUNT.incrementAndGet();
                }
                return call(c, method, args);
            });
    }

    private static Object call(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
