package org.movealong.persistence;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * SessionAdapter is an abstract convenience class which provides a default implementation of
 * {@link #translateException(Exception)}.
 */

public abstract class SessionAdapter<T> implements SessionReceiver<T> {

    /**
     * Translates checked exceptions into unchecked exceptions by returning an UndeclaredThrowableException.
     * Returns e if it is a RuntimeException.
     *
     * @param e the exception thrown from {@link #receive(org.hibernate.classic.Session)}
     * @return e an unchecked exception
     */
    @Override
    public RuntimeException translateException(Exception e) {
        return e instanceof RuntimeException
                ? (RuntimeException) e
                : new UndeclaredThrowableException(e, "Checked exception occurred during session");
    }
}
