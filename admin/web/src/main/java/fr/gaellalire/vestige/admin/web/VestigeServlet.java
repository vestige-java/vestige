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

package fr.gaellalire.vestige.admin.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.VersionUtils;

/**
 * @author Gael Lalire
 */
public class VestigeServlet extends HttpServlet {

    private static final long serialVersionUID = -3972539021994066120L;

    private ApplicationManager applicationManager;

    public VestigeServlet(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        if ("/".equals(requestURI)) {
            requestURI = "index.html";
        } else {
            requestURI = requestURI.substring(1);
        }

        InputStream inputStream = VestigeServlet.class.getResourceAsStream(requestURI);
        if (inputStream == null) {
            super.doGet(req, res);
            return;
        }
        if (requestURI.endsWith(".html")) {
            res.setContentType("text/html");
        } else if (requestURI.endsWith(".js")) {
            res.setContentType("application/javascript");
        } else if (requestURI.endsWith(".gif")) {
            res.setContentType("image/gif");
        } else if (requestURI.endsWith(".png")) {
            res.setContentType("image/png");
        } else if (requestURI.endsWith(".css")) {
            res.setContentType("text/css");
        } else if (requestURI.endsWith(".ico")) {
            res.setContentType("image/x-icon");
        }
        ServletOutputStream outputStream = res.getOutputStream();
        byte[] b = new byte[1024];
        int read = inputStream.read(b);
        while (read != -1) {
            outputStream.write(b, 0, read);
            read = inputStream.read(b);
        }
        outputStream.close();
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            String error = null;
            try {
                String requestURI = req.getRequestURI();
                if ("/mk-repo".equals(requestURI)) {
                    URL url;
                    String s = req.getParameter("url");
                    if (s.endsWith("/")) {
                        url = new URL(s);
                    } else {
                        url = new URL(s + "/");
                    }
                    applicationManager.createRepository(req.getParameter("name"), url);
                } else if ("/rm-repo".equals(requestURI)) {
                    applicationManager.removeRepository(req.getParameter("name"));
                } else if ("/install".equals(requestURI)) {
                    applicationManager.install(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), req.getParameter("local"));
                } else if ("/uninstall".equals(requestURI)) {
                    applicationManager.uninstall(req.getParameter("name"));
                } else if ("/auto-start".equals(requestURI)) {
                    applicationManager.setAutoStarted(req.getParameter("name"), Boolean.parseBoolean(req.getParameter("value")));
                } else if ("/start".equals(requestURI)) {
                    applicationManager.start(req.getParameter("name"));
                } else if ("/stop".equals(requestURI)) {
                    applicationManager.stop(req.getParameter("name"));
                } else if ("/bugfix".equals(requestURI)) {
                    if (Boolean.parseBoolean(req.getParameter("value"))) {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 1);
                    } else {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 0);
                    }
                } else if ("/minor-evolution".equals(requestURI)) {
                    if (Boolean.parseBoolean(req.getParameter("value"))) {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 2);
                    } else {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 0);
                    }
                } else if ("/major-evolution".equals(requestURI)) {
                    if (Boolean.parseBoolean(req.getParameter("value"))) {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 3);
                    } else {
                        applicationManager.setAutoMigrateLevel(req.getParameter("name"), 0);
                    }
                } else if ("/migrate".equals(requestURI)) {
                    applicationManager.migrate(req.getParameter("name"), VersionUtils.fromString(req.getParameter("toVersion")));
                } else if ("/auto-migrate".equals(requestURI)) {
                    applicationManager.autoMigrate();
                } else if ("/get-repos".equals(requestURI)) {
                    Set<String> repositoriesName = applicationManager.getRepositoriesName();
                    JSONArray jsonArray = new JSONArray();
                    for (String string : repositoriesName) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", string);
                        jsonObject.put("url", applicationManager.getRepositoryURL(string).toString());
                        jsonArray.add(jsonObject);
                    }
                    PrintWriter writer = resp.getWriter();
                    writer.print(jsonArray.toJSONString());
                    writer.close();
                    return;
                } else if ("/get-repo-app-name".equals(requestURI)) {
                    Set<String> repositoryApplicationsName = applicationManager.getRepositoryApplicationsName(req.getParameter("repo"));
                    JSONArray jsonArray = new JSONArray();
                    String parameter = req.getParameter("req");
                    for (String string : repositoryApplicationsName) {
                        if (parameter == null || parameter.length() == 0 || string.indexOf(parameter) != -1) {
                            jsonArray.add(string);
                        }
                    }
                    PrintWriter writer = resp.getWriter();
                    writer.print(jsonArray.toJSONString());
                    writer.close();
                    return;
                } else if ("/get-repo-app-version".equals(requestURI)) {
                    String repo = req.getParameter("repo");
                    String name = req.getParameter("name");
                    String exclude = "";
                    if (repo == null || repo.length() == 0) {
                        repo = applicationManager.getRepositoryName(name);
                        exclude = VersionUtils.toString(applicationManager.getRepositoryApplicationVersion(name));
                        name = applicationManager.getRepositoryApplicationName(name);
                    }

                    Set<List<Integer>> repositoryApplicationsName = applicationManager.getRepositoryApplicationVersions(repo, name);
                    JSONArray jsonArray = new JSONArray();
                    String parameter = req.getParameter("req");
                    for (List<Integer> version : repositoryApplicationsName) {
                        String string = VersionUtils.toString(version);
                        if ((parameter == null || parameter.length() == 0 || string.indexOf(parameter) != -1) && !exclude.equals(string)) {
                            jsonArray.add(string);
                        }
                    }
                    PrintWriter writer = resp.getWriter();
                    writer.print(jsonArray.toJSONString());
                    writer.close();
                    return;
                }
            } catch (Exception e) {
                StringWriter out = new StringWriter();
                PrintWriter printWriter = new PrintWriter(out);
                e.printStackTrace(printWriter);
                printWriter.flush();
                error = out.toString();
            }

            JSONObject jsonObject = new JSONObject();
            if (error != null) {
                jsonObject.put("error", error);
            }
            PrintWriter writer = resp.getWriter();
            JSONArray applications = new JSONArray();
            for (String appName : applicationManager.getApplicationsName()) {
                JSONObject jsonApp = new JSONObject();
                jsonApp.put("name", appName);

                String path = applicationManager.getRepositoryName(appName) + "-" + applicationManager.getRepositoryApplicationName(appName) + "-"
                        + VersionUtils.toString(applicationManager.getRepositoryApplicationVersion(appName));
                List<Integer> migrationRepositoryApplicationVersion = applicationManager.getMigrationRepositoryApplicationVersion(appName);
                if (migrationRepositoryApplicationVersion != null) {
                    path += "-" + VersionUtils.toString(migrationRepositoryApplicationVersion);
                }

                jsonApp.put("path", path);
                jsonApp.put("autoStarted", applicationManager.isAutoStarted(appName));
                jsonApp.put("started", applicationManager.isStarted(appName));
                int autoMigrateLevel = applicationManager.getAutoMigrateLevel(appName);
                jsonApp.put("bugfix", autoMigrateLevel >= 1);
                jsonApp.put("minor", autoMigrateLevel >= 2);
                jsonApp.put("major", autoMigrateLevel >= 3);
                applications.add(jsonApp);
            }
            jsonObject.put("application", applications);
            writer.print(jsonObject.toJSONString());
            writer.close();
        } catch (ApplicationException e) {
            throw new ServletException(e);
        }
    }

}
