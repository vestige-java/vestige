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

package fr.gaellalire.vestige.admin.ssh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.CommandLineParser;
import fr.gaellalire.vestige.admin.command.DefaultCommandContext;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.TaskListener;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.history.FileHistory;

/**
 * @author Gael Lalire
 */
public class SSHShellCommand implements Command, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHShellCommand.class);

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    private static final AtomicInteger INPUT_STREAM_THREAD_COUNT = new AtomicInteger(0);

    private InputStream in;

    private LineFilterOutputStream outs;

    private PrintWriter out;

    private PrintWriter err;

    private ConsoleReader consoleReader;

    private ExitCallback callback;

    private SSHShellCommandFactory commandFactory;

    private File historyFile;

    public SSHShellCommand(final SSHShellCommandFactory commandFactory, final File historyFile) {
        this.commandFactory = commandFactory;
        this.historyFile = historyFile;
    }

    public static final int CTRL_C = 3;

    public static final int CTRL_Z = 26;

    /**
     * @author Gael Lalire
     */
    private class AsyncInputStream extends InputStream implements Runnable {

        private volatile boolean asyncMode;

        private ByteArrayOutputStream byteArrayOutputStream;

        private Thread thread;

        AsyncInputStream() {
            byteArrayOutputStream = new ByteArrayOutputStream();
            thread = new Thread(this, "vestige-shell-stream" + INPUT_STREAM_THREAD_COUNT.incrementAndGet());
            thread.start();
        }

        private volatile byte[] byteArray;

        private volatile int pos;

        private volatile boolean reading;

        @Override
        public void run() {
            while (true) {
                if (!asyncMode) {
                    pos = 0;
                    byteArray = byteArrayOutputStream.toByteArray();
                    byteArrayOutputStream.reset();
                    if (readThread != null) {
                        LockSupport.unpark(readThread);
                    }
                    LockSupport.park();
                    continue;
                }
                reading = true;
                try {
                    int read = in.read();
                    if (!asyncMode) {
                        byteArrayOutputStream.write(read);
                        continue;
                    }
                    if (read == CTRL_C) {
                        jobController.interrupt();
                        out.println("^C");
                        out.flush();
                    } else if (read == CTRL_Z) {
                        jobController = null;
                        out.println("^Z");
                        out.flush();
                        asyncMode = false;
                        LockSupport.unpark(SSHShellCommand.this.thread);
                    } else if (jobController != null && read != -1) {
                        out.write(read);
                        out.flush();
                        byteArrayOutputStream.write(read);
                    }
                } catch (IOException e) {
                    LOGGER.error("Console reader exception", e);
                } finally {
                    reading = false;
                }
            }
        }

        private volatile Thread readThread;

        @Override
        public int read() throws IOException {
            if (asyncMode && reading) {
                asyncMode = false;
                readThread = Thread.currentThread();
                while (reading) {
                    LockSupport.park();
                }
            }
            if (byteArray != null) {
                if (pos < byteArray.length) {
                    int c = byteArray[pos];
                    pos++;
                    return c;
                } else {
                    byteArray = null;
                }
            }
            return in.read();
        }

    }

    private Thread thread;

    private FileHistory history;

    private AsyncInputStream asyncInputStream;

    public void start(final Environment env) throws IOException {
        thread = new Thread(this, "vestige-shell-" + THREAD_COUNT.incrementAndGet());
        asyncInputStream = new AsyncInputStream();
        consoleReader = new ConsoleReader(asyncInputStream, outs, new SSHTerminal());
        CandidateListCompletionHandler handler = new CandidateListCompletionHandler();
        handler.setPrintSpaceAfterFullCompletion(false);
        consoleReader.setCompletionHandler(handler);
        history = new FileHistory(historyFile);
        consoleReader.setHistory(history);
        consoleReader.setHistoryEnabled(true);
        consoleReader.setHandleUserInterrupt(true);
        consoleReader.addCompleter(commandFactory.getCompleter());
        thread.start();
    }

    public void setOutputStream(final OutputStream out) {
        this.outs = new LineFilterOutputStream(out);
        this.out = new PrintWriter(outs, true);
    }

    public void setInputStream(final InputStream in) {
        this.in = in;
    }

    public void setExitCallback(final ExitCallback callback) {
        this.callback = callback;
    }

    public void setErrorStream(final OutputStream err) {
        this.err = new PrintWriter(new LineFilterOutputStream(err), true);
    }

    public void destroy() {
        try {
            history.flush();
        } catch (IOException e) {
            LOGGER.warn("Unable to save history", e);
        }
    }

    private JobController jobController = null;

    /**
     * @author Gael Lalire
     */
    private static final class ProgressHolder {
        private float progress = -1;

        private ProgressHolder() {
        }
    }

    public void run() {
        final List<String> descriptions = new ArrayList<String>();
        final List<ProgressHolder> progressHolders = new ArrayList<ProgressHolder>();
        final DefaultCommandContext defaultCommandContext = new DefaultCommandContext();
        CommandLineParser commandLineParser = new CommandLineParser();
        defaultCommandContext.setOut(out);
        StringBuilder sb = new StringBuilder();
        int lastStringBuilderLength = 0;
        try {
            try {
                String readLine = null;
                do {
                    while (jobController != null) {
                        synchronized (descriptions) {
                            for (String description : descriptions) {
                                out.println(description);
                            }
                            boolean first = true;
                            sb.setLength(0);
                            for (ProgressHolder progressHolder : progressHolders) {
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
                            lastStringBuilderLength = sb.length();
                            out.print(sb.toString());
                            out.flush();
                            descriptions.clear();
                        }

                        if (jobController.isDone()) {
                            Exception exception = jobController.getException();
                            if (exception != null) {
                                exception.printStackTrace(out);
                            }
                            jobController = null;
                        } else {
                            asyncInputStream.asyncMode = true;
                            LockSupport.unpark(asyncInputStream.thread);
                            LockSupport.park();
                        }
                        outs.clear(lastStringBuilderLength);
                    }
                    defaultCommandContext.setJobListener(new JobListener() {

                        @Override
                        public void jobDone() {
                            LockSupport.unpark(thread);
                        }

                        @Override
                        public TaskListener taskAdded(final String description) {
                            final JobListener thisJobListener = this;
                            if (defaultCommandContext.getJobListener() == thisJobListener) {
                                final ProgressHolder progressHolder = new ProgressHolder();
                                synchronized (descriptions) {
                                    descriptions.add(description);
                                    progressHolders.add(progressHolder);
                                }
                                LockSupport.unpark(thread);
                                TaskListener taskListener = new TaskListener() {

                                    @Override
                                    public void taskDone() {
                                        if (defaultCommandContext.getJobListener() == thisJobListener) {
                                            synchronized (descriptions) {
                                                progressHolders.remove(progressHolder);
                                            }
                                            LockSupport.unpark(thread);
                                        }
                                    }

                                    @Override
                                    public void progressChanged(final float progress) {
                                        if (defaultCommandContext.getJobListener() == thisJobListener) {
                                            synchronized (descriptions) {
                                                progressHolder.progress = progress;
                                            }
                                            LockSupport.unpark(thread);
                                        }
                                    }
                                };
                                return taskListener;
                            }
                            return null;
                        }
                    });
                    synchronized (descriptions) {
                        // clear after setDoneNotificationHandler which prevent old description to be added
                        descriptions.clear();
                        progressHolders.clear();
                    }
                    try {
                        readLine = consoleReader.readLine("vestige:~ admin$ ");
                    } catch (UserInterruptException e) {
                        continue;
                    }
                    if (readLine == null) {
                        out.println("logout");
                        break;
                    }
                    if (readLine.length() == 0) {
                        continue;
                    }
                    commandLineParser.setCommandLine(readLine);
                    List<String> arguments = new ArrayList<String>();
                    if (commandLineParser.nextArgument()) {
                        String argument = commandLineParser.getUnescapedValue();
                        if ("exit".equals(argument)) {
                            out.println("logout");
                            break;
                        }
                        arguments.add(argument);
                        while (commandLineParser.nextArgument()) {
                            arguments.add(commandLineParser.getUnescapedValue());
                        }
                    } else {
                        continue;
                    }
                    jobController = commandFactory.getVestigeCommandExecutor().exec(defaultCommandContext, arguments);
                } while (true);
                callback.onExit(0);
            } finally {
                out.close();
                err.close();
                in.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Exception with ssh client", e);
        }
    }

}
