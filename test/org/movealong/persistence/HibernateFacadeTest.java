package org.movealong.persistence;

import com.google.inject.*;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Connection;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 8, 2010
 * Time: 10:01:59 PM
 */
@RunWith(JMock.class)
public class HibernateFacadeTest {

    // The object under test
    @Inject private HibernateFacade hibernateFacade;

    // mockery, mocks, and support objects
    private Mockery mockery;
    private SessionFactory sessionFactory;
    private Session session;
    private Connection connection;
    private Transaction transaction;

    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery();
        sessionFactory = mockery.mock(SessionFactory.class, "sessionFactory");
        session = mockery.mock(Session.class, "session");
        connection = mockery.mock(Connection.class, "connection");
        transaction = mockery.mock(Transaction.class, "transaction");

        // like clockwork, all calls to runWithSession and callWithSession should
        // lead to a session being opened and closed, even when if everything
        // possible goes wrong in the Runnable or Callable that's been wrapped.
        mockery.checking(new Expectations() {{
            exactly(1).of(sessionFactory).openSession(); will(returnValue(session));
            exactly(1).of(session).close(); will(returnValue(connection));
        }});

        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SessionFactory.class).toProvider(
                                new Provider<SessionFactory>() {
                                    public SessionFactory get() {
                                        return sessionFactory;
                                    }
                                });
                    }
                });
        injector.injectMembers(this);
    }

    @After
    public void tearDown() throws Exception {
        mockery = null;
        sessionFactory = null;
        session = null;
        connection = null;
        transaction = null;
        hibernateFacade = null;
    }

    @Test
    public void usingSession() throws Exception {
        final Object nonce = new Object();
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            exactly(1).of(receiver).receive(with(sameInstance(session))); will(returnValue(nonce));
            never(receiver).translateException(with(any(Exception.class)));
        }});
        assertSame(nonce, hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() {
                return hibernateFacade.usingSession(receiver);
            }
        }));
    }

    @Test(expected = NoSessionException.class)
    public void usingSessionWithoutSession() throws Exception {
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            never(receiver).receive(with(sameInstance(session)));
            never(receiver).translateException(with(any(Exception.class)));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                // nothing, just need to waste a session to satisfy the default expectations.
            }
        });
        hibernateFacade.usingSession(receiver);
    }

    @Test
    public void usingSessionNested() throws Exception {
        final Object nonce = new Object();
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            exactly(1).of(receiver).receive(with(sameInstance(session))); will(returnValue(nonce));
            never(receiver).translateException(with(any(Exception.class)));
        }});
        assertSame(nonce, hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() {
                return hibernateFacade.usingSession(new NestableSessionReceiver<Object>(hibernateFacade, receiver));
            }
        }));
    }

    @Test(expected = MockUncheckedException.class)
    public void usingSessionReceiverThrowsUnchecked() throws Exception {
        final MockUncheckedException unchecked = new MockUncheckedException();
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            exactly(1).of(receiver).receive(with(sameInstance(session))); will(throwException(unchecked));
            exactly(1).of(receiver).translateException(with(sameInstance(unchecked))); will(returnValue(unchecked));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.usingSession(new NestableSessionReceiver<Object>(hibernateFacade, receiver));
            }
        });
    }

    @Test(expected = MockUncheckedException.class)
    public void usingSessionReceiverThrowsChecked() throws Exception {
        final MockCheckedException checked = new MockCheckedException();
        final MockUncheckedException unchecked = new MockUncheckedException(checked);
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            exactly(1).of(receiver).receive(session); will(throwException(checked));
            exactly(1).of(receiver).translateException(checked); will(throwException(unchecked));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.usingSession(new NestableSessionReceiver<Object>(hibernateFacade, receiver));
            }
        });
    }

    @Test(expected = SessionExistsException.class)
    public void runInSessionNested() throws Exception {
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.runInSession(new Runnable() {
                    public void run() {
                        fail("Execution should not get into this method");
                    }
                });
            }
        });
    }

    @Test
    public void callInSession() throws Exception {
        final Object nonce = new Object();
        assertSame(nonce, hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() throws Exception {
                return nonce;
            }
        }));
    }

    @Test(expected = SessionExistsException.class)
    public void callInSessionNested() throws Exception {
        hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() throws Exception {
                return hibernateFacade.callInSession(new Callable<Object>() {
                    public Object call() throws Exception {
                        fail("Execution should never get this fall.");
                        return null;
                    }
                });
            }
        });
    }

    @Test
    public void inTransaction() throws Exception {
        final Object nonce = new Object();
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("received")); will(returnValue(nonce));
            exactly(1).of(receiver).preCommit(with(sameInstance(session))); when(tx.is("received")); then(tx.is("pre-commit"));
            exactly(1).of(transaction).commit(); when(tx.is("pre-commit")); then(tx.is("committed"));
            exactly(1).of(receiver).postCommit(with(sameInstance(session))); when(tx.is("committed")); then(tx.is("post-commit"));
        }});
        assertSame(nonce, hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() {
                return hibernateFacade.inTransaction(receiver);
            }
        }));
    }

    @Test
    public void inTransactionWithSessionReceiver() throws Exception {
        final Object nonce = new Object();
        final SessionReceiver<Object> receiver = mockery.mock(SessionReceiver.class, "receiver");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); will(returnValue(nonce));
            exactly(1).of(transaction).commit();
        }});
        assertSame(nonce, hibernateFacade.callInSession(new Callable<Object>() {
            public Object call() throws Exception {
                return hibernateFacade.inTransaction(receiver);
            }
        }));
    }

    @Test(expected = TransactionExistsException.class)
    public void inTransactionNested() throws Exception {
        final TransactionReceiver<Object> checkedReceiver = mockery.mock(TransactionReceiver.class, "checkedReceiver");
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(checkedReceiver).receive(with(sameInstance(session))); will(delegateTo(new TransactionAdapter() {
                @Override
                public Object receive(Session session) throws Exception {
                    return hibernateFacade.inTransaction(receiver);
                }
            }));
            exactly(1).of(checkedReceiver).preRollback(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("pre-rollback"));
            exactly(1).of(transaction).rollback(); when(tx.is("pre-rollback")); then(tx.is("rollback"));
            exactly(1).of(checkedReceiver).postRollback(with(sameInstance(session))); when(tx.is("rollback")); then(tx.is("post-rollback"));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.inTransaction(checkedReceiver);
            }
        });
    }

    @Test(expected = MockUncheckedException.class)
    public void inTransactionReceiveThrowsUnchecked() throws Exception {
        final MockUncheckedException unchecked = new MockUncheckedException();
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("received")); will(throwException(unchecked));
            never(receiver).preCommit(with(any(Session.class)));
            never(transaction).commit();
            never(receiver).postCommit(with(any(Session.class)));
            exactly(1).of(receiver).translateException(with(sameInstance(unchecked))); when(tx.is("received")); then(tx.is("translated")); will(returnValue(unchecked));
            exactly(1).of(receiver).preRollback(with(sameInstance(session))); when(tx.is("translated")); then(tx.is("pre-rollback"));
            exactly(1).of(transaction).rollback(); when(tx.is("pre-rollback")); then(tx.is("rollback"));
            exactly(1).of(receiver).postRollback(with(sameInstance(session))); when(tx.is("rollback")); then(tx.is("post-rollback"));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.inTransaction(receiver);
            }
        });
    }

    @Test(expected = MockUncheckedException.class)
    public void inTransactionReceiveThrowsChecked() throws Exception {
        final MockCheckedException checked = new MockCheckedException();
        final MockUncheckedException unchecked = new MockUncheckedException(checked);
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("received")); will(throwException(checked));
            never(receiver).preCommit(with(any(Session.class)));
            never(transaction).commit();
            never(receiver).postCommit(with(any(Session.class)));
            exactly(1).of(receiver).translateException(with(sameInstance(checked))); when(tx.is("received")); then(tx.is("translated")); will(returnValue(unchecked));
            exactly(1).of(receiver).preRollback(with(sameInstance(session))); when(tx.is("translated")); then(tx.is("pre-rollback"));
            exactly(1).of(transaction).rollback(); when(tx.is("pre-rollback")); then(tx.is("rollback"));
            exactly(1).of(receiver).postRollback(with(sameInstance(session))); when(tx.is("rollback")); then(tx.is("post-rollback"));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.inTransaction(receiver);
            }
        });
    }

    @Test(expected = MockUncheckedException.class)
    public void inTransactionRollbackNoCommitOnPreCommitError() throws Exception {
        final Object nonce = new Object();
        final MockUncheckedException unchecked = new MockUncheckedException();
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("received")); will(returnValue(nonce));
            exactly(1).of(receiver).preCommit(with(sameInstance(session))); when(tx.is("received")); then(tx.is("pre-commit")); will(throwException(unchecked));
            never(transaction).commit();
            never(receiver).postCommit(with(any(Session.class)));
            exactly(1).of(receiver).translateException(with(sameInstance(unchecked))); then(tx.is("pre-commit")); then(tx.is("translated")); will(returnValue(unchecked));
            exactly(1).of(receiver).preRollback(with(sameInstance(session))); when(tx.is("translated")); then(tx.is("pre-rollback"));
            exactly(1).of(transaction).rollback(); when(tx.is("pre-rollback")); then(tx.is("rollback"));
            exactly(1).of(receiver).postRollback(with(sameInstance(session))); when(tx.is("rollback")); then(tx.is("post-rollback"));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.inTransaction(receiver);
            }
        });
    }

    @Test(expected = MockUncheckedException.class)
    public void inTransactionRollbackNoCommitOnPreRollbackError() throws Exception {
        final MockUncheckedException receiveError = new MockUncheckedException();
        final MockUncheckedException preRollbackError = new MockUncheckedException();
        final TransactionReceiver<Object> receiver = mockery.mock(TransactionReceiver.class, "receiver");
        final States tx = mockery.states("tx").startsAs("start");
        mockery.checking(new Expectations() {{
            exactly(1).of(session).beginTransaction(); when(tx.is("start")); then(tx.is("begun")); will(returnValue(transaction));
            exactly(1).of(receiver).receive(with(sameInstance(session))); when(tx.is("begun")); then(tx.is("received")); will(throwException(receiveError));
            never(receiver).preCommit(with(any(Session.class)));
            never(transaction).commit();
            never(receiver).postCommit(with(any(Session.class)));
            exactly(1).of(receiver).translateException(with(sameInstance(receiveError))); when(tx.is("received")); then(tx.is("translated")); will(returnValue(receiveError));
            exactly(1).of(receiver).preRollback(with(sameInstance(session))); when(tx.is("translated")); then(tx.is("pre-rollback")); will(throwException(preRollbackError));
            exactly(1).of(transaction).rollback(); when(tx.is("pre-rollback")); then(tx.is("rollback"));
            never(receiver).postRollback(with(any(Session.class)));
        }});
        hibernateFacade.runInSession(new Runnable() {
            public void run() {
                hibernateFacade.inTransaction(receiver);
            }
        });
    }

    private static Action delegateTo(final Object delegate) {
        return new CustomAction("delegated call") {
            public Object invoke(Invocation invocation) throws Throwable {
                if (invocation.getInvokedMethod().getDeclaringClass().isAssignableFrom(delegate.getClass())) {
                    return invocation.applyTo(delegate);
                } else {
                    throw new IllegalArgumentException(delegate + " is not of correct type to accept an invocation of " + invocation);
                }
            }
        };
    }

    private static class NestableSessionReceiver<T> implements SessionReceiver<T> {
        private final SessionReceiver<T> nested;
        private final HibernateFacade hibernateFacade;

        public NestableSessionReceiver(HibernateFacade hibernateFacade, SessionReceiver<T> nested) {
            this.nested = nested;
            this.hibernateFacade = hibernateFacade;
        }

        @Override
        public T receive(Session session) {
            assertNotNull("The session should never be null", session);
            return hibernateFacade.usingSession(nested);
        }

        @Override
        public RuntimeException translateException(Exception e) {
            if (e instanceof RuntimeException) {
                return (RuntimeException) e;
            } else {
                fail("The checked exception should have already been translated by the nested receiver");
                return null;
            }
        }
    }

    private static class MockUncheckedException extends RuntimeException {
        private MockUncheckedException() {
            super();
        }

        public MockUncheckedException(Exception e) {
            super(e);
        }
    }

    private static class MockCheckedException extends Exception {}

}
