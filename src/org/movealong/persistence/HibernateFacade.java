package org.movealong.persistence;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

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
     * @param <T> the type of the receiver.receive(Session) return value
     * @return the return value of receiver.receive(Session)
     * @throws HibernateException if no open session exists for the calling thread.
     */
    public <T> T usingSession(SessionReceiver<T> receiver) throws HibernateException {
        Session session = sessionLocal.get();
        if (session == null) {
            throw new NoSessionException("usingSession(..) must be called under a higher call to inSession.");
        }
        try {
            return receiver.receive(session);
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
     * Creates a Runnable whose run method will properly open and close a {@link Session} around a call to
     * the supplied runner's run method.
     *
     * @param runner a Runnable
     * @return a Runnable that will call runner.run() after opening a Session and properly close it after the
     * call ends either by returning or throwing an exception.
     */
    public Runnable inSession(final Runnable runner) {
        return new Runnable() {
            @Override
            public void run() {
                Session session = sessionLocal.get();
                if (session != null) {
                    throw new SessionExistsException("Called with an existing session");
                }
                sessionLocal.set(getSessionFactory().openSession());
                try {
                    runner.run();
                } finally {
                    sessionLocal.get().close();
                    sessionLocal.remove();
                }
            }
        };
    }

    /**
     * Creates a Callable whose run method will properly open and close a {@link Session} around a call to
     * the supplied call's call method.
     *
     * @param call a Callable
     * @return a Callable that will call call.call() after opening a Session and properly close it after the
     * call ends either by returning or throwing an exception.
     */
    public <T> Callable<T> inSession(final Callable<T> call) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                Session session = sessionLocal.get();
                if (session != null) {
                    throw new SessionExistsException("Called with an existing session");
                }
                sessionLocal.set(getSessionFactory().openSession());
                try {
                    return call.call();
                } finally {
                    sessionLocal.get().close();
                    sessionLocal.remove();
                }
            }
        };
    }

    /**
     * Call's the supplied Runnable's run method after opening a Hibernate session and associating it
     * with the calling thread, and properly closes the session after the call to run ends.
     *
     * @param runner a Runnable that will have its run method called immediately with a Hibernate session
     * associated with the thread.
     * @throws org.hibernate.HibernateException if called in a thread that already has a Session
     * associated with it.
     */
    public void runInSession(final Runnable runner) throws HibernateException {
        inSession(runner).run();
    }

    /**
     * Call's the supplied Callable's call method after opening a Hibernate session and associating it
     * with the calling thread, and properly closes the session after the call to run ends.
     *
     * @param call a Runnable that will have its run method called immediately with a Hibernate session
     * associated with the thread.
     * @return the value returned by call.call()
     * @throws HibernateException if called in a thread that already has a Session
     * associated with it.
     * @throws Exception as thrown from call.call()
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
     * @param <T> the return type of receiver.receive()
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
     * @param <T> the return type of receiver.receive()
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

}
