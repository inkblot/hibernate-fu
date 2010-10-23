package org.movealong.persistence;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.movealong.persistence.test.entity.SomeEntity;
import org.movealong.persistence.test.entity.SomeOtherEntity;

import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Oct 21, 2010
 * Time: 6:56:20 PM
 */
public class XmlSessionFactoryProviderTest {

    @Test
    public void testStuff() throws Exception {
        Injector injector = Guice.createInjector(
                new HibernateFacadeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        HibernateFacadeModule.addXmlConfigurationResource(binder(), "org/movealong/persistence/test/hibernate.one.xml");
                        HibernateFacadeModule.addXmlConfigurationResource(binder(), "org/movealong/persistence/test/hibernate.two.xml");
                    }
                });
        Provider<SessionFactory> provider = injector.getProvider(SessionFactory.class);
        SessionFactory sessionFactory = provider.get();
        assertNotNull(sessionFactory.getClassMetadata(SomeEntity.class));
        assertNotNull(sessionFactory.getClassMetadata(SomeOtherEntity.class));
    }
}
