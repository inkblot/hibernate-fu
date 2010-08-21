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
    private final String configurationFile;

    public HibernateFacadeModule() {
        this(null);
    }

    public HibernateFacadeModule(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    protected void configure() {
        bind(SessionFactory.class).toProvider(new XmlSessionFactoryProvider(configurationFile));
    }
}
