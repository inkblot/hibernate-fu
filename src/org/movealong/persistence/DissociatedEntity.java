package org.movealong.persistence;

import org.hibernate.classic.Session;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 21, 2010
 * Time: 4:42:18 AM
 */
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
