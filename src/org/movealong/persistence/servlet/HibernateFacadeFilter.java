package org.movealong.persistence.servlet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.movealong.persistence.HibernateFacade;

import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Aug 21, 2010
 * Time: 8:04:43 AM
 */
public class HibernateFacadeFilter implements Filter {

    @Inject
    private HibernateFacade hibernate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Injector injector = (Injector) filterConfig.getServletContext().getAttribute("injector");
        if (injector == null) {
            throw new ServletException("Servlet attribute not bound to a Guice injector: injector");
        }
        injector.injectMembers(this);
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        try {
            hibernate.runInSession(new Runnable() {
                @Override
                public void run() {
                    try {
                        filterChain.doFilter(servletRequest, servletResponse);
                    } catch (IOException e) {
                        throw new UndeclaredThrowableException(e);
                    } catch (ServletException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
            });
        } catch (UndeclaredThrowableException e) {
            if (e.getUndeclaredThrowable() instanceof IOException) {
                throw (IOException) e.getUndeclaredThrowable();
            } else if (e.getUndeclaredThrowable() instanceof ServletException) {
                throw (ServletException) e.getUndeclaredThrowable();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void destroy() {
        hibernate = null;
    }

}
