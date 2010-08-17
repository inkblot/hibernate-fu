package org.movealong.persistence;

/**
 * Thrown to indicate that no Session is associated with the current thread when calling
 * a method of {@link HibernateFacade} which requires one.
 */
public class NoSessionException extends HibernateFacadeException {
    public NoSessionException(String s) {
        super(s);
    }
}
