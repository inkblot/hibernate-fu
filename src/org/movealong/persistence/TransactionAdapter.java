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
