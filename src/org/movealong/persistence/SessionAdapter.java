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
