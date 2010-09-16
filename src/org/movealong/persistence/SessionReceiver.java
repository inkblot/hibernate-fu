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
 * The SessionReceiver interface defines the methods and calling contract of code that will run with an open
 * Hibernate session when passed to {@link HibernateFacade#usingSession(SessionReceiver)}. The intended use
 * of this interface is that library clients will implement it to contain their persistence business logic.
 */
public interface SessionReceiver<T> {
    /**
     * Called from inside of {@link HibernateFacade#usingSession(SessionReceiver)} when called with a session
     * opened in the thread.
     *
     * @param session an open Hibernate session
     * @return the result of persistence operations within the SessionReceiver.
     * @throws Exception as necessary from the implementer's persistence code
     */
    T receive(Session session) throws Exception;

    /**
     * Called from inside of {@link HibernateFacade#usingSession(SessionReceiver)} after
     * {@link #receive(Session)} has thrown any exception.  The RuntimeException that it returns may not be
     * null, and will be thrown from usingSession(SessionReceiver).
     *
     * @param e the exception thrown from {@link #receive(org.hibernate.classic.Session)}
     * @return a RuntimeException of the implementer's choosing
     */
    RuntimeException translateException(Exception e);
}
