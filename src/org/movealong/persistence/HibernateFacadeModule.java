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
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.hibernate.SessionFactory;
import org.movealong.persistence.servlet.HibernateFacadeFilter;

/**
 * Creates Guice bindings for a HibernateFacade using a Hibernate SessionFactory configured using the named XML
 * configuration file.
 */
public class HibernateFacadeModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SessionFactory.class).toProvider(XmlSessionFactoryProvider.class);
        requestStaticInjection(HibernateFacadeFilter.class);
    }

    public static void addXmlConfigurationResource(Binder binder, String configurationFile) {
        Multibinder.newSetBinder(binder, String.class, Names.named(XmlSessionFactoryProvider.DEFAULT_CONFIGURATION_FILE))
                .addBinding().toInstance(configurationFile);
    }
}
