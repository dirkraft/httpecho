package com.github.dirkraft.httpecho;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Ø {

    public static <T> Supplier<T> supplier(Callable<T> c) {
        return () -> {
            try {
                return c.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <V> V call(Callable<V> c) {
        try {
            return c.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
