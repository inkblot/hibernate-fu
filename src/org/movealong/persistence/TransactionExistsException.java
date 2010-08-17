package org.movealong.persistence;

/**
 * Thrown to indicate that the current thread is already in a transaction when calling
 * a method of {@link HibernateFacade} that starts a transaction.
 */
public class TransactionExistsException extends HibernateFacadeException {
    public TransactionExistsException(String s) {
        super(s);
    }
}
