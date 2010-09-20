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

import com.google.inject.AbstractModule;
import org.hibernate.SessionFactory;

/**
 * Creates Guice bindings for a HibernateFacade using a Hibernate SessionFactory configured using the named XML
 * configuration file.
 */
public class HibernateFacadeModule extends AbstractModule {
    private final String[] configurationFiles;

    public HibernateFacadeModule() {
        this((String[]) null);
    }

    public HibernateFacadeModule(String... configurationFiles) {
        this.configurationFiles = configurationFiles;
    }

    @Override
    protected void configure() {
        bind(SessionFactory.class).toProvider(new XmlSessionFactoryProvider(configurationFiles));
    }
}
