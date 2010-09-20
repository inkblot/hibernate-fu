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

import com.google.inject.Provider;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link Provider<SessionFactory>} which creates the SessionFactory using a the XML configuration file
 * named in the constructor.  In lieu of naming a configuration file, the provider uses the default name
 * <code>/hibernate.cfg.xml</code>.
 */
public class XmlSessionFactoryProvider implements Provider<SessionFactory> {
    /**
     * The default configuration file name, used when another is not specified using the constructor.
     */
    public static final String DEFAULT_CONFIGURATION_FILE = "/hibernate.cfg.xml";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String[] configurationFiles;
    private SessionFactory sessionFactory;

    public XmlSessionFactoryProvider() {
        this((String[]) null);
    }

    public XmlSessionFactoryProvider(String... configurationFiles) {
        this.configurationFiles = configurationFiles == null
                ? new String[] { DEFAULT_CONFIGURATION_FILE }
                : configurationFiles;
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
        AnnotationConfiguration configuration = new AnnotationConfiguration();
        for (String configurationFile : configurationFiles) {
            configuration.configure(configurationFile);
        }
        return configuration.buildSessionFactory();
    }
}
