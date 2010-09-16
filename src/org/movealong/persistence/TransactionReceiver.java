/*
 * (c) Copyright 2010 Nate Riffe <inkblot@movealong.org>
 *
 * This file is part of movealong-hibernate.
 *
 * movealong-hibernate is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * movealong-hibernate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with movealong-hibernate.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
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
