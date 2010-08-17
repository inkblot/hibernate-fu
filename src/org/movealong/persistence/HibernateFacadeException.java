package org.movealong.persistence;

import org.hibernate.HibernateException;

/**
 * The super class of all exceptions that can be thrown from misuse of {@link HibernateFacade} 
 */
public class HibernateFacadeException extends HibernateException {
    public HibernateFacadeException(String s) {
        super(s);
    }
}
