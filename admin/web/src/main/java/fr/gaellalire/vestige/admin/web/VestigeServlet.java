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
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.CommandLineParser;
import fr.gaellalire.vestige.admin.command.DefaultCommandContext;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.ApplicationManagerState;
import fr.gaellalire.vestige.application.manager.ApplicationManagerStateListener;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.TaskListener;

/**
 * @author Gael Lalire
 */
public class VestigeServlet extends WebSocketServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeServlet.class);

    private static final long serialVersionUID = -3972539021994066120L;

    private ApplicationManager applicationManager;

    private VestigeCommandExecutor vestigeCommandExecutor;

    private WebTerminalCommandCompleter webTerminalCommandCompleter;

    private Map<String, CommandHandler> commandHandlerByName = new HashMap<String, CommandHandler>();

    public VestigeServlet(final ApplicationManager applicationManager, final VestigeCommandExecutor vestigeCommandExecutor) {
        this.applicationManager = applicationManager;
        this.vestigeCommandExecutor = vestigeCommandExecutor;
        webTerminalCommandCompleter = new WebTerminalCommandCompleter(vestigeCommandExecutor.getCommandByNames());
        commandHandlerByName.put("mk-repo", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                URL url;
                String s = (String) jsonObject.get("url");
                if (s.endsWith("/")) {
                    url = new URL(s);
                } else {
                    url = new URL(s + "/");
                }
                applicationManager.createRepository((String) jsonObject.get("name"), url);
                return null;
            }
        });
        commandHandlerByName.put("rm-repo", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                applicationManager.removeRepository((String) jsonObject.get("name"));
                return null;
            }
        });
        commandHandlerByName.put("install", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                return applicationManager.install((String) jsonObject.get("repo"), (String) jsonObject.get("name"), VersionUtils.fromString((String) jsonObject.get("version")),
                        (String) jsonObject.get("local"), jobListener);
            }
        });
        commandHandlerByName.put("uninstall", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                return applicationManager.uninstall((String) jsonObject.get("name"), jobListener);
            }
        });
        commandHandlerByName.put("auto-start", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                applicationManager.setAutoStarted((String) jsonObject.get("name"), ((Boolean) jsonObject.get("value")).booleanValue());
                return null;
            }
        });
        commandHandlerByName.put("start", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                applicationManager.start((String) jsonObject.get("name"));
                return null;
            }
        });
        commandHandlerByName.put("stop", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                return applicationManager.stop((String) jsonObject.get("name"), jobListener);
            }
        });
        commandHandlerByName.put("bugfix", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                if (((Boolean) jsonObject.get("value")).booleanValue()) {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 1);
                } else {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 0);
                }
                return null;
            }
        });
        commandHandlerByName.put("minor-evolution", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                if (((Boolean) jsonObject.get("value")).booleanValue()) {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 2);
                } else {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 0);
                }
                return null;
            }
        });
        commandHandlerByName.put("major-evolution", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject value, final JobListener jobListener) throws Exception {
                JSONObject jsonObject = value;
                if (((Boolean) jsonObject.get("value")).booleanValue()) {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 3);
                } else {
                    applicationManager.setAutoMigrateLevel((String) jsonObject.get("name"), 0);
                }
                return null;
            }
        });
        commandHandlerByName.put("migrate", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject jsonObject, final JobListener jobListener) throws Exception {
                return applicationManager.migrate((String) jsonObject.get("name"), VersionUtils.fromString((String) jsonObject.get("toVersion")), jobListener);
            }
        });
        commandHandlerByName.put("auto-migrate", new CommandHandler() {

            @Override
            public JobController execute(final JSONObject value, final JobListener jobListener) throws Exception {
                return applicationManager.autoMigrate(jobListener);
            }
        });
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

    /**
     * @author Gael Lalire
     */
    private static final class ProgressHolder {
        private float progress = -1;

        private ProgressHolder() {
        }
    }

    /**
     * @author Gael Lalire
     */
    private interface CommandHandler {

        JobController execute(JSONObject value, JobListener jobListener) throws Exception;

    }

    @Override
    public WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {
        final List<ProgressHolder> termProgressHolders = new ArrayList<ProgressHolder>();
        final List<ProgressHolder> guiProgressHolders = new ArrayList<ProgressHolder>();
        final JSONObject jsonObject = new JSONObject();
        return new WebSocket.OnTextMessage() {

            private Thread updater;

            private Connection connection;

            private ApplicationManagerState lastState;

            private List<String> guiDescriptions;

            private JobController termJobController;

            private JobController guiJobController;

            private PrintWriter termPrintWriter;

            private JobListener guiJobListener;

            private JobListener termJobListener;

            private ApplicationManagerStateListener listener;

            @Override
            public void onOpen(final Connection connection) {
                guiDescriptions = new ArrayList<String>();
                this.connection = connection;
                listener = new ApplicationManagerStateListener() {

                    @Override
                    public void stateChanged(final ApplicationManagerState state) {
                        synchronized (jsonObject) {
                            lastState = state;
                            jsonObject.notify();
                        }
                    }
                };
                applicationManager.addStateListener(listener);
                try {
                    synchronized (jsonObject) {
                        lastState = applicationManager.copyState();
                        jsonObject.notify();
                    }
                } catch (ApplicationException e) {
                    LOGGER.error("Unable to get state", e);
                }
                termPrintWriter = new PrintWriter(new Writer() {

                    private StringBuilder stringBuilder = new StringBuilder();

                    @Override
                    public void write(final char[] cbuf, final int off, final int len) throws IOException {
                        stringBuilder.append(cbuf, off, len);
                    }

                    @Override
                    public void flush() throws IOException {
                        synchronized (jsonObject) {
                            if (stringBuilder.length() != 0) {
                                String[] split = stringBuilder.toString().split("\r?\n");
                                JSONArray array = new JSONArray();
                                for (String s : split) {
                                    array.add(s);
                                }
                                jsonObject.put("termEcho", array);
                                try {
                                    connection.sendMessage(jsonObject.toJSONString());
                                } finally {
                                    jsonObject.clear();
                                }
                                stringBuilder.setLength(0);
                            }
                        }
                    }

                    @Override
                    public void close() throws IOException {

                    }
                }, true);
                connection.setMaxIdleTime(0);
                updater = new Thread("vestige-servlet-updater") {
                    @Override
                    public void run() {
                        synchronized (jsonObject) {
                            try {
                                long lastSend = 0;
                                StringBuilder sb = new StringBuilder();
                                while (true) {
                                    long nsend = System.currentTimeMillis();
                                    long diff = nsend - lastSend;
                                    while (diff != 0 && diff < 150) {
                                        // delay
                                        jsonObject.wait(diff);
                                        nsend = System.currentTimeMillis();
                                        diff = nsend - lastSend;
                                    }
                                    lastSend = nsend;
                                    if (termJobController != null) {
                                        if (termJobController.isDone()) {
                                            Exception exception = termJobController.getException();
                                            if (exception != null) {
                                                exception.printStackTrace(termPrintWriter);
                                            }
                                            jsonObject.put("termDone", "1");
                                            termJobController = null;
                                        } else {
                                            sb.setLength(0);
                                            boolean first = true;
                                            for (ProgressHolder progressHolder : termProgressHolders) {
                                                if (progressHolder.progress < 0) {
                                                    continue;
                                                }
                                                if (first) {
                                                    first = false;
                                                } else {
                                                    sb.append(" ");
                                                }
                                                sb.append((int) (progressHolder.progress * 100));
                                                sb.append("%");
                                            }
                                            jsonObject.put("termTmpEcho", sb.toString());
                                        }
                                    }
                                    if (guiJobController != null) {
                                        if (guiJobController.isDone()) {
                                            Exception exception = guiJobController.getException();
                                            if (exception != null) {
                                                StringWriter out = new StringWriter();
                                                PrintWriter printWriter = new PrintWriter(out);
                                                exception.printStackTrace(printWriter);
                                                printWriter.flush();
                                                jsonObject.put("guiError", out.toString());
                                            }
                                            jsonObject.put("guiDone", "1");
                                            guiJobController = null;
                                        } else {
                                            JSONObject tasks = new JSONObject();
                                            JSONArray guiDescriptionsArray = new JSONArray();
                                            for (String description : guiDescriptions) {
                                                guiDescriptionsArray.add(description);
                                            }
                                            tasks.put("descriptions", guiDescriptionsArray);
                                            guiDescriptions.clear();
                                            JSONArray guiProgress = new JSONArray();
                                            for (ProgressHolder progressHolder : guiProgressHolders) {
                                                if (progressHolder.progress < 0) {
                                                    guiProgress.add(Boolean.FALSE);
                                                } else {
                                                    guiProgress.add((int) (progressHolder.progress * 100));
                                                }
                                            }
                                            tasks.put("progress", guiProgress);
                                            jsonObject.put("guiTasks", tasks);
                                        }
                                    }
                                    if (lastState != null) {
                                        try {
                                            JSONArray applications = new JSONArray();
                                            for (String appName : lastState.getApplicationsName()) {
                                                JSONObject jsonApp = new JSONObject();
                                                jsonApp.put("name", appName);

                                                String path = lastState.getRepositoryName(appName) + "-" + lastState.getRepositoryApplicationName(appName) + "-"
                                                        + VersionUtils.toString(lastState.getRepositoryApplicationVersion(appName));
                                                List<Integer> migrationRepositoryApplicationVersion = lastState.getMigrationRepositoryApplicationVersion(appName);
                                                if (migrationRepositoryApplicationVersion != null && migrationRepositoryApplicationVersion.size() != 0) {
                                                    path += "->" + VersionUtils.toString(migrationRepositoryApplicationVersion);
                                                }

                                                jsonApp.put("path", path);
                                                jsonApp.put("autoStarted", lastState.isAutoStarted(appName));
                                                jsonApp.put("started", lastState.isStarted(appName));
                                                int autoMigrateLevel = lastState.getAutoMigrateLevel(appName);
                                                jsonApp.put("bugfix", autoMigrateLevel >= 1);
                                                jsonApp.put("minor", autoMigrateLevel >= 2);
                                                jsonApp.put("major", autoMigrateLevel >= 3);
                                                applications.add(jsonApp);
                                            }
                                            jsonObject.put("application", applications);
                                        } catch (ApplicationException e) {
                                            LOGGER.error("Unable to get state", e);
                                        }
                                        lastState = null;
                                    }
                                    connection.sendMessage(jsonObject.toJSONString());
                                    jsonObject.clear();
                                    do {
                                        jsonObject.wait();
                                    } while (lastState == null && guiJobController == null && termJobController == null);
                                }
                            } catch (IOException e) {
                                LOGGER.error("Updater exception", e);
                            } catch (InterruptedException e) {
                                LOGGER.debug("Updater interrupted, quit", e);
                            } finally {
                                jsonObject.clear();
                            }
                        }
                    }
                };
                updater.start();
            }

            @Override
            public void onClose(final int closeCode, final String message) {
                applicationManager.removeStateListener(listener);
                connection.close();
                updater.interrupt();
            }

            @Override
            public void onMessage(final String value) {
                JSONObject parsedJsonObject = (JSONObject) JSONValue.parse(value);

                if (parsedJsonObject.get("guiBackground") != null) {
                    synchronized (jsonObject) {
                        if (guiJobController != null) {
                            jsonObject.put("guiDone", "1");
                            try {
                                connection.sendMessage(jsonObject.toJSONString());
                            } catch (IOException e) {
                                LOGGER.error("Unable to send guiDone", e);
                            } finally {
                                jsonObject.clear();
                            }
                            guiJobController = null;
                            guiJobListener = null;
                            guiDescriptions.clear();
                            guiProgressHolders.clear();
                        }
                    }
                }

                if (parsedJsonObject.get("guiInterrupt") != null) {
                    synchronized (jsonObject) {
                        if (guiJobController != null) {
                            guiJobController.interrupt();
                        }
                    }
                }

                ApplicationManagerState applicationManagerState = null;
                String complete = (String) parsedJsonObject.get("complete");
                if (complete != null) {
                    try {
                        JSONObject completeArgs = (JSONObject) parsedJsonObject.get("args");
                        synchronized (jsonObject) {
                            JSONObject completeResponse = new JSONObject();
                            jsonObject.put("completeResponse", completeResponse);

                            completeResponse.put("command", complete);
                            if ("get-repos".equals(complete)) {
                                applicationManagerState = applicationManager.copyState();
                                Set<String> repositoriesName = applicationManagerState.getRepositoriesName();
                                JSONArray jsonArray = new JSONArray();
                                for (String string : repositoriesName) {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("name", string);
                                    jsonObject.put("url", applicationManagerState.getRepositoryURL(string).toString());
                                    jsonArray.add(jsonObject);
                                }
                                completeResponse.put("args", jsonArray);
                            } else if ("get-repo-app-name".equals(complete)) {
                                Set<String> repositoryApplicationsName = applicationManager.getRepositoryMetadata((String) completeArgs.get("repo")).listApplicationsName();
                                JSONArray jsonArray = new JSONArray();
                                String parameter = (String) completeArgs.get("req");
                                for (String string : repositoryApplicationsName) {
                                    if (parameter == null || parameter.length() == 0 || string.indexOf(parameter) != -1) {
                                        jsonArray.add(string);
                                    }
                                }
                                completeResponse.put("args", jsonArray);
                            } else if ("get-repo-app-version".equals(complete)) {
                                applicationManagerState = applicationManager.copyState();
                                String repo = (String) completeArgs.get("repo");
                                String name = (String) completeArgs.get("name");
                                String exclude = "";
                                JSONArray jsonArray = new JSONArray();
                                if (name != null && name.length() != 0) {
                                    if (repo == null || repo.length() == 0) {
                                        repo = applicationManagerState.getRepositoryName(name);
                                        exclude = VersionUtils.toString(applicationManagerState.getRepositoryApplicationVersion(name));
                                        name = applicationManagerState.getRepositoryApplicationName(name);
                                    }

                                    Set<List<Integer>> repositoryApplicationsVersions = applicationManager.getRepositoryMetadata(repo).listApplicationVersions(name);
                                    String parameter = (String) completeArgs.get("req");
                                    for (List<Integer> version : repositoryApplicationsVersions) {
                                        String string = VersionUtils.toString(version);
                                        if ((parameter == null || parameter.length() == 0 || string.indexOf(parameter) != -1) && !exclude.equals(string)) {
                                            jsonArray.add(string);
                                        }
                                    }
                                }
                                completeResponse.put("args", jsonArray);
                            } else if ("termComplete".equals(complete)) {
                                JSONArray candidates = new JSONArray();
                                webTerminalCommandCompleter.complete((String) completeArgs.get("buffer"), ((Integer) completeArgs.get("cursor")).intValue(), candidates);
                                completeResponse.put("args", candidates);
                            }
                            try {
                                connection.sendMessage(jsonObject.toJSONString());
                            } catch (IOException e) {
                                LOGGER.error("Unable to send guiDone", e);
                            } finally {
                                jsonObject.clear();
                            }
                        }
                    } catch (ApplicationException e) {
                        LOGGER.error("Unable to complete", e);
                    }
                }

                String guiCommand = (String) parsedJsonObject.get("guiCommand");
                if (guiCommand != null) {
                    try {
                        guiJobListener = new JobListener() {

                            @Override
                            public TaskListener taskAdded(final String description) {
                                final JobListener thisJobListener = this;
                                final ProgressHolder progressHolder;
                                synchronized (jsonObject) {
                                    if (thisJobListener != guiJobListener) {
                                        return null;
                                    }
                                    guiDescriptions.add(description);
                                    progressHolder = new ProgressHolder();
                                    guiProgressHolders.add(progressHolder);
                                    jsonObject.notify();
                                }
                                return new TaskListener() {

                                    @Override
                                    public void taskDone() {
                                        if (thisJobListener == guiJobListener) {
                                            synchronized (jsonObject) {
                                                progressHolder.progress = 1;
                                                jsonObject.notify();
                                            }
                                        }
                                    }

                                    @Override
                                    public void progressChanged(final float progress) {
                                        if (thisJobListener == guiJobListener) {
                                            progressHolder.progress = progress;
                                            synchronized (jsonObject) {
                                                jsonObject.notify();
                                            }
                                        }
                                    }
                                };
                            }

                            @Override
                            public void jobDone() {
                                synchronized (jsonObject) {
                                    if (this == guiJobListener) {
                                        jsonObject.notify();
                                        guiDescriptions.clear();
                                        guiProgressHolders.clear();
                                    }
                                }
                            }
                        };
                        guiJobController = commandHandlerByName.get(guiCommand).execute((JSONObject) parsedJsonObject.get("guiArgs"), guiJobListener);
                        synchronized (jsonObject) {
                            if (guiJobController != null) {
                                jsonObject.put("guiJob", guiJobController.getDescription());
                                try {
                                    connection.sendMessage(jsonObject.toJSONString());
                                } catch (IOException ie) {
                                    LOGGER.error("Unable to send guiJob", ie);
                                } finally {
                                    jsonObject.clear();
                                }
                            }
                        }
                    } catch (Exception e) {
                        StringWriter out = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(out);
                        e.printStackTrace(printWriter);
                        printWriter.flush();
                        jsonObject.put("guiError", out.toString());
                        try {
                            connection.sendMessage(jsonObject.toJSONString());
                        } catch (IOException ie) {
                            LOGGER.error("Unable to send guiError", ie);
                        } finally {
                            jsonObject.clear();
                        }
                    }
                }

                if (parsedJsonObject.get("termBackground") != null) {
                    synchronized (jsonObject) {
                        if (termJobController != null) {
                            jsonObject.put("termDone", "1");
                            try {
                                connection.sendMessage(jsonObject.toJSONString());
                            } catch (IOException e) {
                                LOGGER.error("Unable to send guiDone", e);
                            } finally {
                                jsonObject.clear();
                            }
                            termJobController = null;
                            termJobListener = null;
                            termProgressHolders.clear();
                        }
                    }
                }

                if (parsedJsonObject.get("termInterrupt") != null) {
                    synchronized (jsonObject) {
                        if (termJobController != null) {
                            termJobController.interrupt();
                        }
                    }
                }

                String termCommand = (String) parsedJsonObject.get("termCommand");
                if (termCommand != null) {
                    final DefaultCommandContext commandContext = new DefaultCommandContext();
                    final CommandLineParser commandLineParser = new CommandLineParser();
                    commandContext.setOut(termPrintWriter);
                    termJobListener = new JobListener() {

                        @Override
                        public TaskListener taskAdded(final String description) {
                            final JobListener thisJobListener = this;
                            if (thisJobListener != termJobListener) {
                                return null;
                            }
                            termPrintWriter.println(description);
                            final ProgressHolder progressHolder = new ProgressHolder();
                            synchronized (jsonObject) {
                                termProgressHolders.add(progressHolder);
                                jsonObject.notify();
                            }
                            return new TaskListener() {

                                @Override
                                public void taskDone() {
                                    if (thisJobListener != termJobListener) {
                                        return;
                                    }
                                    synchronized (jsonObject) {
                                        termProgressHolders.remove(progressHolder);
                                        jsonObject.notify();
                                    }
                                }

                                @Override
                                public void progressChanged(final float progress) {
                                    progressHolder.progress = progress;
                                    synchronized (jsonObject) {
                                        jsonObject.notify();
                                    }
                                }
                            };
                        }

                        @Override
                        public void jobDone() {
                            synchronized (jsonObject) {
                                jsonObject.notify();
                            }
                        }
                    };
                    commandContext.setJobListener(termJobListener);
                    commandLineParser.setCommandLine(termCommand);
                    List<String> arguments = new ArrayList<String>();
                    while (commandLineParser.nextArgument()) {
                        arguments.add(commandLineParser.getUnescapedValue());
                    }

                    JobController termJobController = vestigeCommandExecutor.exec(commandContext, arguments);
                    synchronized (jsonObject) {
                        if (termJobController == null) {
                            try {
                                jsonObject.put("termDone", "1");
                                connection.sendMessage(jsonObject.toJSONString());
                            } catch (IOException e) {
                                LOGGER.error("Unable to send termDone", e);
                            } finally {
                                jsonObject.clear();
                            }
                        } else {
                            this.termJobController = termJobController;
                        }
                    }
                    termPrintWriter.flush();
                }
            }

        };
    }
}
