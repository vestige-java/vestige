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

package com.googlecode.vestige.admin.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.application.VersionUtils;

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
        synchronized (applicationManager) {
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
                        applicationManager.install(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")));
                    } else if ("/uninstall".equals(requestURI)) {
                        applicationManager.uninstall(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")));
                    } else if ("/start".equals(requestURI)) {
                        applicationManager.start(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")));
                    } else if ("/stop".equals(requestURI)) {
                        applicationManager.stop(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")));
                    } else if ("/bugfix".equals(requestURI)) {
                        if (Boolean.parseBoolean(req.getParameter("value"))) {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 1);
                        } else {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 0);
                        }
                    } else if ("/minor-evolution".equals(requestURI)) {
                        if (Boolean.parseBoolean(req.getParameter("value"))) {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 2);
                        } else {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 0);
                        }
                    } else if ("/major-evolution".equals(requestURI)) {
                        if (Boolean.parseBoolean(req.getParameter("value"))) {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 3);
                        } else {
                            applicationManager.setAutoMigrateLevel(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("version")), 0);
                        }
                    } else if ("/migrate".equals(requestURI)) {
                        applicationManager.migrate(req.getParameter("repo"), req.getParameter("name"), VersionUtils.fromString(req.getParameter("fromVersion")),
                                VersionUtils.fromString(req.getParameter("toVersion")));
                    } else if ("/auto-migrate".equals(requestURI)) {
                        applicationManager.autoMigrate();
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
                JSONArray repositories = new JSONArray();

                PrintWriter writer = resp.getWriter();
                for (String repoName : applicationManager.getRepositoriesName()) {
                    JSONObject jsonRepo = new JSONObject();
                    jsonRepo.put("name", repoName);
                    URL repositoryURL = applicationManager.getRepositoryURL(repoName);
                    jsonRepo.put("url", repositoryURL.toString());
                    JSONArray applications = new JSONArray();
                    for (String appName : applicationManager.getApplicationsName(repoName)) {
                        JSONObject jsonApp = new JSONObject();
                        jsonApp.put("name", appName);
                        JSONArray versions = new JSONArray();
                        for (List<Integer> version : applicationManager.getVersions(repoName, appName)) {
                            JSONObject jsonVersion = new JSONObject();
                            jsonVersion.put("value", VersionUtils.toString(version));
                            jsonVersion.put("started", applicationManager.isStarted(repoName, appName, version));
                            int autoMigrateLevel = applicationManager.getAutoMigrateLevel(repoName, appName, version);
                            jsonVersion.put("bugfix", autoMigrateLevel >= 1);
                            jsonVersion.put("minor", autoMigrateLevel >= 2);
                            jsonVersion.put("major", autoMigrateLevel >= 3);
                            versions.add(jsonVersion);
                        }
                        jsonApp.put("version", versions);
                        applications.add(jsonApp);
                    }
                    jsonRepo.put("application", applications);
                    repositories.add(jsonRepo);
                }
                jsonObject.put("repository", repositories);
                writer.print(jsonObject.toJSONString());
                writer.close();
            } catch (ApplicationException e) {
                throw new ServletException(e);
            }
        }
    }

}
