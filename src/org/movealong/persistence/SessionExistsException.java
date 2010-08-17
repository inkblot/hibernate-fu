package org.movealong.persistence;

/**
 * Thrown to indicate that there is a session associated with the current thread when
 * calling a method of {@link HibernateFacade} which associates a session with a thread.
 */
public class SessionExistsException extends HibernateFacadeException {
    public SessionExistsException(String s) {
        super(s);
    }
}
