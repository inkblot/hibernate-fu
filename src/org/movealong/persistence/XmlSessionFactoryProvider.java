package org.movealong.persistence;

import com.google.inject.Provider;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 14, 2010
 * Time: 10:43:35 AM
 */
public class XmlSessionFactoryProvider implements Provider<SessionFactory> {
    public final String DEFAULT_CONFIGURATION_FILE = "/hibernate.cfg.xml";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String configurationFile;
    private SessionFactory sessionFactory;

    public XmlSessionFactoryProvider() {
        this(null);
    }

    public XmlSessionFactoryProvider(String configurationFile) {
        this.configurationFile = configurationFile == null
                ? DEFAULT_CONFIGURATION_FILE
                : configurationFile;
    }

    @Override
    public SessionFactory get() {
        lock.readLock().lock();
        try {
            if (sessionFactory == null) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    sessionFactory = createSessionFactory();
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            return sessionFactory;
        } finally {
            lock.readLock().unlock();
        }
    }

    private SessionFactory createSessionFactory() {
        return new AnnotationConfiguration()
                .configure(configurationFile)
                .buildSessionFactory();
    }
}
