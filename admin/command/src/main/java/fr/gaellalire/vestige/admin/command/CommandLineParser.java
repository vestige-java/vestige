package fr.gaellalire.vestige.admin.command;

/**
 * @author Gael Lalire
 */
public class CommandLineParser {

    private String commandLine;

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
        start = 0;
        end = 0;
        mode = 0;
        unescapeValue = "";
    }

    public boolean nextArgument() {
        return nextArgument(false);
    }

    public boolean nextArgument(final boolean addEmpty) {
        // mode 0: normal, 1:single quote, 2 double quote, 3 normal
        // backslash, 4 double quote backslash
        int i = end;
        if (end == commandLine.length()) {
            return false;
        }
        loop: while (i < commandLine.length()) {
            switch (commandLine.charAt(i)) {
            case ' ':
            case ' ':
            case '\t':
            case '\n':
            case '\f':
            case '\r':
                break;
            default:
                start = i;
                break loop;
            }
            i++;
        }
        mode = 0;
        if (i == commandLine.length()) {
            if (addEmpty) {
                unescapeValue = "";
                start = i;
                end = i;
                return true;
            } else {
                return false;
            }
        }
        StringBuilder unescape = new StringBuilder();
        int from = start;
        loop: while (i < commandLine.length()) {
            switch (commandLine.charAt(i)) {
            case '\'':
                if (mode == 0) {
                    unescape.append(commandLine.substring(from, i));
                    from = i + 1;
                    mode = 1;
                } else if (mode == 1) {
                    unescape.append(commandLine.substring(from, i));
                    mode = 0;
                    from = i + 1;
                } else if (mode == 3) {
                    // backslash
                    mode = 0;
                } else if (mode == 4) {
                    unescape.append('\\');
                    mode = 2;
                }
                break;
            case '"':
                if (mode == 0) {
                    unescape.append(commandLine.substring(from, i));
                    from = i + 1;
                    mode = 2;
                } else if (mode == 2) {
                    unescape.append(commandLine.substring(from, i));
                    mode = 0;
                    from = i + 1;
                } else if (mode == 3) {
                    mode = 0;
                } else if (mode == 4) {
                    mode = 2;
                }
                break;
            case '\\':
                if (mode == 0) {
                    unescape.append(commandLine.substring(from, i));
                    from = i + 1;
                    mode = 3;
                } else if (mode == 2) {
                    unescape.append(commandLine.substring(from, i));
                    from = i + 1;
                    mode = 4;
                } else if (mode == 3) {
                    unescape.append('\\');
                    from = i + 1;
                    mode = 0;
                } else if (mode == 4) {
                    unescape.append('\\');
                    from = i + 1;
                    mode = 2;
                }
                break;
            case ' ':
            case ' ':
            case '\t':
            case '\n':
            case '\f':
            case '\r':
                if (mode == 0) {
                    break loop;
                } else if (mode == 3) {
                    mode = 0;
                } else if (mode == 4) {
                    unescape.append('\\');
                    mode = 2;
                }
                break;
            default:
                if (mode == 3) {
                    mode = 0;
                } else if (mode == 4) {
                    unescape.append('\\');
                    mode = 2;
                }
                break;
            }
            i++;
        }
        end = i;
        unescape.append(commandLine.substring(from, end));
        unescapeValue = unescape.toString();
        return true;
    }

    private String unescapeValue;

    private int mode;

    private int start;

    private int end;

    public int getMode() {
        return mode;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getUnescapedValue() {
        return unescapeValue;
    }

    public static String escape(final int mode, final String s) {
        StringBuilder escape = new StringBuilder();
        if (mode == 0 || mode == 3) {
            for (int i = 0; i < s.length(); i++) {
                char charAt = s.charAt(i);
                switch (charAt) {
                case '\'':
                case '"':
                case '\\':
                case ' ':
                case ' ':
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                    if (mode != 3 || i != 0) {
                        escape.append('\\');
                    }
                    break;
                default:
                    break;
                }
                escape.append(charAt);
            }
        } else if (mode == 1) {
            // single quote
            for (int i = 0; i < s.length(); i++) {
                char charAt = s.charAt(i);
                switch (charAt) {
                case '\'':
                    escape.append("'\\'");
                    break;
                default:
                    break;
                }
                escape.append(charAt);
            }
            escape.append('\'');
        } else if (mode == 2 || mode == 4) {
            // double quote
            for (int i = 0; i < s.length(); i++) {
                char charAt = s.charAt(i);
                switch (charAt) {
                case '"':
                case '\\':
                    if (mode != 4 || i != 0) {
                        escape.append('\\');
                    }
                    break;
                default:
                    break;
                }
                escape.append(charAt);
            }
            escape.append('"');
        }
        return escape.toString();
    }


    public String getEscapeSuffixValue(final int unescapePosition) {
        if (unescapePosition == 0) {
            return commandLine.substring(start, end);
        }
        int unescapeCurrentPosition = 0;
        int i = start;
        int mode = 0;
        while (unescapeCurrentPosition <= unescapePosition) {
            if (i == commandLine.length()) {
                i++;
                break;
            }
            switch (commandLine.charAt(i)) {
            case '\'':
                if (mode == 0) {
                    mode = 1;
                } else if (mode == 1) {
                    mode = 0;
                } else if (mode == 3) {
                    // backslash
                    unescapeCurrentPosition++;
                    mode = 0;
                } else if (mode == 4) {
                    unescapeCurrentPosition++;
                    mode = 2;
                } else {
                    unescapeCurrentPosition++;
                }
                break;
            case '"':
                if (mode == 0) {
                    mode = 2;
                } else if (mode == 2) {
                    mode = 0;
                } else if (mode == 3) {
                    unescapeCurrentPosition++;
                    mode = 0;
                } else if (mode == 4) {
                    unescapeCurrentPosition++;
                    mode = 2;
                } else {
                    unescapeCurrentPosition++;
                }
                break;
            case '\\':
                if (mode == 0) {
                    mode = 3;
                } else if (mode == 2) {
                    mode = 4;
                } else if (mode == 3) {
                    unescapeCurrentPosition++;
                    mode = 0;
                } else if (mode == 4) {
                    unescapeCurrentPosition++;
                    mode = 2;
                } else {
                    unescapeCurrentPosition++;
                }
                break;
            case ' ':
            case ' ':
            case '\t':
            case '\n':
            case '\f':
            case '\r':
                if (mode == 3) {
                    mode = 0;
                    unescapeCurrentPosition++;
                } else if (mode == 4) {
                    mode = 2;
                    unescapeCurrentPosition += 2;
                } else {
                    unescapeCurrentPosition++;
                }
                break;
            default:
                if (mode == 3) {
                    mode = 0;
                    unescapeCurrentPosition++;
                } else if (mode == 4) {
                    mode = 2;
                    unescapeCurrentPosition += 2;
                } else {
                    unescapeCurrentPosition++;
                }
                break;
            }
            i++;
        }
        return commandLine.substring(i - 1, end);
    }

}
