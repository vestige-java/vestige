/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.vestige.edition.standard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.admin.web.VestigeServlet;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.edition.standard.schema.Bind;
import com.googlecode.vestige.edition.standard.schema.Web;

/**
 * @author Gael Lalire
 */
public class WebServerFactory implements Callable<Server> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerFactory.class);

    private Web web;

    private ApplicationManager applicationManager;

    @SuppressWarnings("unused")
    private File appHomeFile;

    public WebServerFactory(final Web web, final ApplicationManager applicationManager, final File appHomeFile) {
        this.web = web;
        this.applicationManager = applicationManager;
        this.appHomeFile = appHomeFile;
    }

    public Server call() throws Exception {
        List<Bind> binds = web.getBind();
        Server webServer = new Server();
        List<Connector> connectors = new ArrayList<Connector>(binds.size());
        for (Bind bind : binds) {
            Connector connector = new SocketConnector();
            String host = bind.getHost();
            connector.setPort(bind.getPort());
            connector.setHost(host);
            if (LOGGER.isInfoEnabled()) {
                if (host == null) {
                    host = "*";
                }
                LOGGER.info("Listen on {}:{} for web interface", host, bind.getPort());
            }
            connectors.add(connector);
        }
        webServer.setConnectors(connectors.toArray(new Connector[connectors.size()]));
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(new ServletHolder(new VestigeServlet(applicationManager)), "/");

//        String contextPath = "webdav";
//        HttpManagerBuilder httpManagerBuilder = new HttpManagerBuilder();
//        httpManagerBuilder.setFsContextPath(contextPath);
//        httpManagerBuilder.setFsHomeDir(appHomeFile.getPath());
//        servletHandler.addServletWithMapping(new ServletHolder(new VestigeWebdavServlet(httpManagerBuilder.buildHttpManager())),
//                "/" + contextPath + "/*");

        webServer.setHandler(servletHandler);
        return webServer;
    }

    public static void main(final String[] args) {
    }

}
