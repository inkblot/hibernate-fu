package org.movealong.persistence;

import org.hibernate.classic.Session;

/**
 * TransactionAdapter is an abstract convenience class which provides default implementations of the transaction
 * state callback hooks.
 */
public abstract class TransactionAdapter<T> extends SessionAdapter<T> implements TransactionReceiver<T> {
    /**
     * Returns without doing anything.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    @Override
    public void preCommit(Session session) {
    }

    /**
     * Returns without doing anything.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    @Override
    public void postCommit(Session session) {
    }

    /**
     * Returns without doing anything.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    @Override
    public void preRollback(Session session) {
    }

    /**
     * Returns without doing anything.
     *
     * @param session the same open session as passed to {@link #receive(Session)}
     */
    @Override
    public void postRollback(Session session) {
    }
}
