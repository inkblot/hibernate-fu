package org.movealong.persistence;

import com.google.inject.*;
import org.hibernate.SessionFactory;
import org.jmock.Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.movealong.junitfu.JUnitFu;
import org.movealong.junitfu.Modules;
import org.movealong.persistence.test.entity.SomeEntity;
import org.movealong.persistence.test.entity.SomeOtherEntity;

import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Oct 21, 2010
 * Time: 6:56:20 PM
 */
@RunWith(JUnitFu.class)
@Modules({HibernateFacadeModule.class, XmlSessionFactoryProviderTest.TestModule.class})
public class XmlSessionFactoryProviderTest {

    @Inject public Mockery mock;
    @Inject public SessionFactory sessionFactory;

    @Test
    public void testStuff() throws Exception {
        assertNotNull(sessionFactory.getClassMetadata(SomeEntity.class));
        assertNotNull(sessionFactory.getClassMetadata(SomeOtherEntity.class));
    }

    public static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            HibernateFacadeModule.addXmlConfigurationResource(binder(), "org/movealong/persistence/test/hibernate.one.xml");
            HibernateFacadeModule.addXmlConfigurationResource(binder(), "org/movealong/persistence/test/hibernate.two.xml");
        }
    }
}
