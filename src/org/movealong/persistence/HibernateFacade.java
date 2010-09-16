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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

/**
 * HibernateFacade enforces proper creation, closure, and disposal of Hibernate sessions and transactions.  It
 * uses a {@link SessionFactory} instance provided via Guice injection using a {@link Provider}.
 */
@Singleton
public class HibernateFacade {

    private Provider<SessionFactory> sessionFactoryProvider;
    private ThreadLocal<Session> sessionLocal = new ThreadLocal<Session>();
    private ThreadLocal<Transaction> txLocal = new ThreadLocal<Transaction>();

    @Inject
    public HibernateFacade(Provider<SessionFactory> sessionFactoryProvider) {
        this.sessionFactoryProvider = sessionFactoryProvider;
    }

    /**
     * Gets the existing open Hibernate session for the current thread and calls {@link SessionReceiver}'s
     * interface methods according to the specification in that interface's documentation.
     *
     * @param receiver the SessionReceiver that will use the open Session
     * @param <T>      the type of the receiver.receive(Session) return value
     * @return the return value of receiver.receive(Session)
     * @throws HibernateException if no open session exists for the calling thread.
     */
    public <T> T usingSession(SessionReceiver<T> receiver) throws HibernateException {
        try {
            return receiver.receive(getSession());
        } catch (HibernateFacadeException e) {
            // HibernateFacadeExceptions are thrown higher up the call stack
            // when another call to the facade singleton generates an error
            // at the facade level.  The receiver does not get an opportunity
            // to translate these.
            throw e;
        } catch (Exception e) {
            throw receiver.translateException(e);
        }
    }

    /**
     * Accesses the {@link Session} associated with the current call stack.
     * @return the {@link Session} associated with the current call stack.
     * @throws HibernateException if there is no session associate with the current call stack.
     */
    public Session getSession() {
        Session session = sessionLocal.get();
        if (session == null) {
            throw new NoSessionException("There is no session associated with the current call stack." +
                    "  Sessions are managed by a call to inSession at a point higher on the call stack.");
        }
        return session;
    }

    /**
     * Creates a Runnable whose run method will properly open and close a {@link Session} around a call to
     * the supplied runner's run method.
     *
     * @param runner a Runnable
     * @return a Runnable that will call runner.run() after opening a Session and properly close it after the
     *         call ends either by returning or throwing an exception.
     */
    public Runnable inSession(final Runnable runner) {
        return new RunnableCallable(inSession(new CallableRunnable(runner)));
    }

    /**
     * Creates a Callable whose call method will properly open and close a {@link Session} around a call to
     * the supplied call's call method.
     *
     * @param call a Callable
     * @return a Callable that will call call.call() after opening a Session and properly close it after the
     *         call ends either by returning or throwing an exception.
     */
    public <T> Callable<T> inSession(final Callable<T> call) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                Session session;
                try {
                    getSession();
                    throw new SessionExistsException("Called with an existing session");
                } catch (NoSessionException e) {
                    session = getSessionFactory().openSession();
                }
                sessionLocal.set(session);
                try {
                    return call.call();
                } finally {
                    sessionLocal.remove();
                    session.close();
                }
            }
        };
    }

    /**
     * Calls the supplied Runnable's run method after opening a Hibernate session and associating it
     * with the calling thread, and properly closes the session after the call to run ends.
     *
     * @param runner a Runnable that will have its run method called immediately with a Hibernate session
     *               associated with the thread.
     * @throws org.hibernate.HibernateException
     *          if called in a thread that already has a Session associated with it.
     */
    public void runInSession(final Runnable runner) throws HibernateException {
        inSession(runner).run();
    }

    /**
     * Calls the call method of the supplied Callable after opening a Hibernate session and associating it
     * with the calling thread, and properly closes the session after the call to call ends.
     *
     * @param call a Runnable that will have its run method called immediately with a Hibernate session
     *             associated with the thread.
     * @return the value returned by call.call()
     * @throws HibernateException if called in a thread that already has a Session
     *                            associated with it.
     * @throws Exception          as thrown from call.call()
     */
    public <T> T callInSession(final Callable<T> call) throws Exception {
        return inSession(call).call();
    }

    /**
     * Gets the calling thread's open Session and passes it to receiver.receive() after starting
     * a transaction.  The transaction will be properly committed or rolled back after the call
     * to receiver.receive() ends.
     *
     * @param receiver the receiver that will be provided with the thread's open Session
     * @param <T>      the return type of receiver.receive()
     * @return the value of receiver.receive()
     * @throws HibernateException if called in a thread that has no Session associated with it
     */
    public <T> T inTransaction(final SessionReceiver<T> receiver) throws HibernateException {
        return inTransaction(
                new TransactionAdapter<T>() {
                    @Override
                    public T receive(Session session) throws Exception {
                        return receiver.receive(session);
                    }
                });
    }

    /**
     * Gets the calling thread's open Session and passes it to receiver.receive() after starting
     * a transaction.  The transaction will be properly committed or rolled back after the call
     * to receiver.receive() ends, with calls to the preCommit(), postCommit(), preRollback(),
     * and postRollback() methods of receiver as specified for {@link TransactionReceiver}.
     *
     * @param receiver the receiver that will be provided with the thread's open Session
     * @param <T>      the return type of receiver.receive()
     * @return the value of receiver.receive()
     * @throws HibernateException if called in a thread that has no Session associated with it
     */
    public <T> T inTransaction(final TransactionReceiver<T> receiver) throws HibernateException {
        return usingSession(
                new SessionAdapter<T>() {
                    @Override
                    public T receive(Session session) {
                        Transaction transaction = txLocal.get();
                        if (transaction == null) {
                            transaction = session.beginTransaction();
                            txLocal.set(transaction);
                        } else {
                            throw new TransactionExistsException("Current thread is already in a transaction");
                        }

                        T result;
                        try {
                            result = receiver.receive(session);
                            receiver.preCommit(session);
                            transaction.commit();
                        } catch (TransactionExistsException e) {
                            // TransactionExistsException is thrown higher up the call stack
                            // when another call to inTransaction is made during the current
                            // frame's call to receiver.receive(..).  We're not going to let
                            // the receiver translate it.
                            handleRollback(session, transaction);
                            throw e;
                        } catch (Exception e) {
                            RuntimeException exception = receiver.translateException(e);
                            handleRollback(session, transaction);
                            throw exception;
                        } finally {
                            txLocal.remove();
                        }
                        receiver.postCommit(session);
                        return result;
                    }

                    private void handleRollback(Session session, Transaction transaction) {
                        try {
                            receiver.preRollback(session);
                        } finally {
                            transaction.rollback();
                        }
                        receiver.postRollback(session);
                    }
                });
    }

    private SessionFactory getSessionFactory() {
        return sessionFactoryProvider.get();
    }


    private static class CallableRunnable implements Callable<Void> {
        private final Runnable runner;

        public CallableRunnable(Runnable runner) {
            this.runner = runner;
        }

        @Override
        public Void call() {
            runner.run();
            return null;
        }
    }

    private static class RunnableCallable implements Runnable {
        private final Callable<Void> call;

        public RunnableCallable(Callable<Void> call) {
            this.call = call;
        }

        @Override
        public void run() {
            try {
                call.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Exception e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
}
