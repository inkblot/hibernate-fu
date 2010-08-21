package org.movealong.persistence;

import com.google.inject.*;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.movealong.common.util.Functional;

import java.sql.Connection;
import java.util.concurrent.Callable;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 21, 2010
 * Time: 4:57:45 AM
 */
@RunWith(JMock.class)
public class DissociatedEntityTest {
    private Mockery mockery;
    private SessionFactory sessionFactory;
    private Session session;
    private Connection connection;

    @Inject
    private HibernateFacade hibernate;

    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery();
        sessionFactory = mockery.mock(SessionFactory.class, "sessionFactory");
        session = mockery.mock(Session.class, "session");
        connection = mockery.mock(Connection.class, "connection");

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
        sessionFactory = null;
        session = null;
        connection = null;
        mockery = null;
        hibernate = null;
    }

    @Test
    public void dissociate() throws Exception {
        final Object nonce = new Object();
        final Functional.Lambda<Object> lambda = mockery.mock(Functional.Lambda.class, "lambda");
        final MockEntity entity = new MockEntity(lambda);
        mockery.checking(new Expectations() {{
            exactly(1).of(session).get(MockEntity.class, 0); will(returnValue(entity));
            exactly(1).of(lambda).apply(nonce);
        }});

        hibernate.runInSession(
                new Runnable() {
                    @Override
                    public void run() {
                        Functional.Lambda<Object> dissociatedEntity = DissociatedEntity.dissociate(hibernate, MockEntity.class, 0);
                        dissociatedEntity.apply(nonce);
                    }
                });
    }

    private class MockEntity implements Functional.Lambda<Object> {
        private final Functional.Lambda<Object> lambda;

        public MockEntity(Functional.Lambda<Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public void apply(Object o) {
            lambda.apply(o);
        }
    }
}
