package fr.gaellalire.vestige.admin.command.argument;

import java.io.File;

/**
 * @author Gael Lalire
 */
public class FileArgument implements Argument {

    private File file;

    @Override
    public void parse(final String s) throws ParseException {
        file = new File(s);
    }

    @Override
    public void propose(final ProposeContext proposeContext) throws ParseException {
        String prefix = proposeContext.getPrefix();
        File f = new File(prefix);
        if (!prefix.endsWith(File.separator)) {
            f = f.getParentFile();
        }
        if (f != null && f.isDirectory()) {
            int hiddenPathLength;
            String hiddenPath = f.getPath();
            if (hiddenPath.endsWith(File.separator)) {
                hiddenPathLength = hiddenPath.length();
            } else {
                hiddenPathLength = hiddenPath.length() + 1;
            }
            proposeContext.setUnescapePrefixHiddenLength(hiddenPathLength);
            for (File subFile : f.listFiles()) {
                if (subFile.isDirectory()) {
                    proposeContext.addUnterminatedProposition(subFile.getPath() + File.separator);
                } else {
                    proposeContext.addProposition(subFile.getPath());
                }
            }
        }
    }

    @Override
    public void reset() {
        file = null;
    }

    @Override
    public String getName() {
        return "file";
    }

    public File getFile() {
        return file;
    }

}
