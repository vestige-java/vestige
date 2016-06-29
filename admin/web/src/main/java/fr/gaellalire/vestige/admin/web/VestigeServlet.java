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
import net.minidev.json.JSONValue;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.ApplicationManagerState;
import fr.gaellalire.vestige.application.manager.VersionUtils;

/**
 * @author Gael Lalire
 */
public class VestigeServlet extends HttpServlet {

    private static final long serialVersionUID = -3972539021994066120L;

    private ApplicationManager applicationManager;

    private VestigeCommandExecutor vestigeCommandExecutor;

    private WebTerminalCommandCompleter webTerminalCommandCompleter;

    public VestigeServlet(final ApplicationManager applicationManager, final VestigeCommandExecutor vestigeCommandExecutor) {
        this.applicationManager = applicationManager;
        this.vestigeCommandExecutor = vestigeCommandExecutor;
        webTerminalCommandCompleter = new WebTerminalCommandCompleter(vestigeCommandExecutor.getCommandByNames());
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
            ApplicationManagerState applicationManagerState;
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
                    applicationManagerState = applicationManager.copyState();
                    Set<String> repositoriesName = applicationManagerState.getRepositoriesName();
                    JSONArray jsonArray = new JSONArray();
                    for (String string : repositoriesName) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", string);
                        jsonObject.put("url", applicationManagerState.getRepositoryURL(string).toString());
                        jsonArray.add(jsonObject);
                    }
                    PrintWriter writer = resp.getWriter();
                    writer.print(jsonArray.toJSONString());
                    writer.close();
                    return;
                } else if ("/get-repo-app-name".equals(requestURI)) {
                    Set<String> repositoryApplicationsName = applicationManager.getRepositoryMetadata(req.getParameter("repo")).listApplicationsName();
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
                    applicationManagerState = applicationManager.copyState();
                    String repo = req.getParameter("repo");
                    String name = req.getParameter("name");
                    String exclude = "";
                    JSONArray jsonArray = new JSONArray();
                    PrintWriter writer = resp.getWriter();
                    if (name == null || name.length() == 0) {
                        writer.print(jsonArray.toJSONString());
                        writer.close();
                        return;
                    }
                    if (repo == null || repo.length() == 0) {
                        repo = applicationManagerState.getRepositoryName(name);
                        exclude = VersionUtils.toString(applicationManagerState.getRepositoryApplicationVersion(name));
                        name = applicationManagerState.getRepositoryApplicationName(name);
                    }

                    Set<List<Integer>> repositoryApplicationsVersions = applicationManager.getRepositoryMetadata(repo).listApplicationVersions(name);
                    String parameter = req.getParameter("req");
                    for (List<Integer> version : repositoryApplicationsVersions) {
                        String string = VersionUtils.toString(version);
                        if ((parameter == null || parameter.length() == 0 || string.indexOf(parameter) != -1) && !exclude.equals(string)) {
                            jsonArray.add(string);
                        }
                    }
                    writer.print(jsonArray.toJSONString());
                    writer.close();
                    return;
                } else if ("/complete".equals(requestURI)) {
                    JSONArray candidates = new JSONArray();
                    webTerminalCommandCompleter.complete(req.getParameter("buffer"), Integer.parseInt(req.getParameter("cursor")), candidates);
                    PrintWriter writer = resp.getWriter();
                    writer.print(candidates.toJSONString());
                    writer.close();
                    return;
                } else if ("/execute".equals(requestURI)) {
                    StringWriter outStringWriter = new StringWriter();
                    vestigeCommandExecutor.exec(new PrintWriter(outStringWriter), req.getParameter("command").split("\\s+"));
                    PrintWriter writer = resp.getWriter();
                    writer.print(JSONValue.toJSONString(outStringWriter.toString()));
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
            applicationManagerState = applicationManager.copyState();
            for (String appName : applicationManagerState.getApplicationsName()) {
                JSONObject jsonApp = new JSONObject();
                jsonApp.put("name", appName);

                String path = applicationManagerState.getRepositoryName(appName) + "-" + applicationManagerState.getRepositoryApplicationName(appName) + "-"
                        + VersionUtils.toString(applicationManagerState.getRepositoryApplicationVersion(appName));
                List<Integer> migrationRepositoryApplicationVersion = applicationManagerState.getMigrationRepositoryApplicationVersion(appName);
                if (migrationRepositoryApplicationVersion != null && migrationRepositoryApplicationVersion.size() != 0) {
                    path += "-" + VersionUtils.toString(migrationRepositoryApplicationVersion);
                }

                jsonApp.put("path", path);
                jsonApp.put("autoStarted", applicationManagerState.isAutoStarted(appName));
                jsonApp.put("started", applicationManagerState.isStarted(appName));
                int autoMigrateLevel = applicationManagerState.getAutoMigrateLevel(appName);
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
