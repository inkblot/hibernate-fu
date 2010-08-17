package org.movealong.persistence;

import com.google.inject.AbstractModule;
import org.hibernate.SessionFactory;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 14, 2010
 * Time: 10:27:56 AM
 */
public class HibernateFacadeModule extends AbstractModule {
    private final String propertiesFile;

    public HibernateFacadeModule() {
        this(null);
    }

    public HibernateFacadeModule(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    @Override
    protected void configure() {
        bind(SessionFactory.class).toProvider(new XmlSessionFactoryProvider(propertiesFile));
    }
}
