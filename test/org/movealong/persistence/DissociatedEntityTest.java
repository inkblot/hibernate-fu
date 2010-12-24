package org.movealong.persistence;

import com.google.inject.*;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.movealong.common.util.Functional;
import org.movealong.junitfu.JUnitFu;
import org.movealong.junitfu.Mock;

import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 21, 2010
 * Time: 4:57:45 AM
 */
@RunWith(JUnitFu.class)
public class DissociatedEntityTest {

    // the object under test
    @Inject public HibernateFacade hibernate;

    // mockery, mocks, and support objects
    @Inject public Mockery mockery;
    @Inject @Mock public SessionFactory sessionFactory;
    @Inject @Mock public Session session;
    @Inject @Mock public Connection connection;

    @Before
    public void setUp() throws Exception {
        // like clockwork, all calls to runWithSession and callWithSession should
        // lead to a session being opened and closed, even when if everything
        // possible goes wrong in the Runnable or Callable that's been wrapped.
        mockery.checking(new Expectations() {{
            exactly(1).of(sessionFactory).openSession(); will(returnValue(session));
            exactly(1).of(session).close(); will(returnValue(connection));
        }});
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
