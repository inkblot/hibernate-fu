package org.movealong.persistence;

import org.hibernate.classic.Session;

/**
 * The TransactionReceiver interface is an extension of the {@link SessionReceiver} interface with additional
 * callback hooks for executing client code around transactional state changes.
 */
public interface TransactionReceiver<T> extends SessionReceiver<T> {
    /**
     * Called after {@link #receive(Session)} prior to transaction commit inside of
     * {@link HibernateFacade#inTransaction(TransactionReceiver)}, and given the same open Hibernate session.
     * Any exceptions that are thrown from preCommit(Session) will abort commit of the transaction and cause
     * rollback to begin.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    void preCommit(Session session);

    /**
     * Called after {@link #receive(Session)} after transaction commit inside of
     * {@link HibernateFacade#inTransaction(TransactionReceiver)}, and given the same open Hibernate session.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    void postCommit(Session session);

    /**
     * Called after {@link #receive(Session)} or {@link #preCommit(Session)} throw an exception, prior to
     * transaction rollback inside of {@link HibernateFacade#inTransaction(TransactionReceiver)}, and given
     * the same open Hibernate session.  Rollback will proceed even if preRollback(Session) throws an
     * exception, but {@link #postRollback(Session)} will not be called.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    void preRollback(Session session);

    /**
     * Called after {@link #receive(Session)} or {@link #preCommit(Session)} throw an exception, after
     * transaction rollback inside of {@link HibernateFacade#inTransaction(TransactionReceiver)}, and given
     * the same open Hibernate session.  Will not be called if {@link #preRollback(Session)} threw an
     * exception.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    void postRollback(Session session);
}
