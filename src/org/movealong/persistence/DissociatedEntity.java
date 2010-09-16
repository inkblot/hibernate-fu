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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DissociatedEntity {

    private DissociatedEntity() {}

    public static <E> E dissociate(HibernateFacade hibernateFacade, Class<E> entityClass, Serializable id) {
        return (E) Proxy.newProxyInstance(entityClass.getClassLoader(), entityClass.getInterfaces(), new DissociationHandler(hibernateFacade, entityClass, id));
    }

    private static class DissociationHandler implements InvocationHandler {
        private final HibernateFacade hibernateFacade;
        private final Class<?> entityClass;
        private final Serializable id;

        public DissociationHandler(HibernateFacade hibernateFacade, Class<?> entityClass, Serializable id) {
            this.hibernateFacade = hibernateFacade;
            this.entityClass = entityClass;
            this.id = id;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(
                    hibernateFacade.usingSession(
                            new SessionAdapter<Object>() {
                                @Override
                                public Object receive(Session session) throws Exception {
                                    return session.get(entityClass, id);
                                }
                            }),
                    args);
        }
    }

}
