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

package fr.gaellalire.vestige.edition.standard;

import java.io.File;
import java.io.IOException;
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

import fr.gaellalire.vestige.admin.web.VestigeServlet;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.Web;

/**
 * @author Gael Lalire
 */
public class WebServerFactory implements Callable<VestigeServer> {

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

    public VestigeServer call() throws Exception {
        List<Bind> binds = web.getBind();
        final Server webServer = new Server();
        List<Connector> connectors = new ArrayList<Connector>(binds.size());
        for (final Bind bind : binds) {
            final String host = bind.getHost();

            Connector connector = new SocketConnector() {
                @Override
                public void open() throws IOException {
                    super.open();
                    if (LOGGER.isInfoEnabled()) {
                        if (host == null) {
                            LOGGER.info("Listen on *:{} for web interface", bind.getPort());
                        } else {
                            LOGGER.info("Listen on {}:{} for web interface", host, bind.getPort());
                        }
                    }
                }
            };
            connector.setPort(bind.getPort());
            connector.setHost(host);
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
        return new VestigeServer() {

            @Override
            public void stop() throws Exception {
                webServer.stop();
                LOGGER.info("Web interface stopped");
            }

            @Override
            public void start() throws Exception {
                webServer.start();
                LOGGER.info("Web interface started");
            }
        };
    }

}
