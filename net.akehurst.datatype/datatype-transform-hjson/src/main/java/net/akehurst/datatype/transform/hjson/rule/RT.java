package net.akehurst.datatype.transform.hjson.rule;

import java.util.concurrent.Callable;

public class RT {
    public static void wrap(final Runnable action) {
        try {
            action.run();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T wrap(final Callable<T> action) {
        try {
            return action.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
