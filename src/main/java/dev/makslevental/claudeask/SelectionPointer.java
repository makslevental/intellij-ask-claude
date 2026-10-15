package dev.makslevental.claudeask;

/**
 * Builds the Claude Code file reference for a range of lines: {@code @src/Foo.kt#L10-20}, or
 * {@code @src/Foo.kt#L12} for a single line. Pure, so it can be tested without the IDE.
 */
public final class SelectionPointer {
    private SelectionPointer() {}

    /**
     * @param path      the file path to show: relative to the project root when the file is inside it,
     *                  otherwise absolute. Uses forward slashes.
     * @param firstLine first line, 1-based
     * @param lastLine  last line, 1-based, at least {@code firstLine}
     */
    public static String build(String path, int firstLine, int lastLine) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path is empty");
        if (firstLine < 1) throw new IllegalArgumentException("lines are 1-based: " + firstLine);
        int last = Math.max(firstLine, lastLine);
        String lines = last == firstLine ? "L" + firstLine : "L" + firstLine + "-" + last;
        return "@" + path.replace('\\', '/') + "#" + lines;
    }

    /**
     * The last line a selection really covers. A selection that ends at column 0 of a line (the usual
     * result of selecting whole lines) does not include that line.
     *
     * @param startLine0 0-based line of the selection start
     * @param endLine0   0-based line of the selection end
     * @param endColumn  0-based column of the selection end
     * @param hasSelection false when only a caret is placed
     * @return the 1-based last line
     */
    public static int lastLine(int startLine0, int endLine0, int endColumn, boolean hasSelection) {
        if (!hasSelection) return startLine0 + 1;
        if (endColumn == 0 && endLine0 > startLine0) return endLine0;
        return endLine0 + 1;
    }

    /**
     * The path to show for a file: relative to {@code projectBase} when the file lies inside it,
     * otherwise the absolute path.
     */
    public static String displayPath(String absolutePath, String projectBase) {
        String file = absolutePath.replace('\\', '/');
        if (projectBase == null || projectBase.isBlank()) return file;
        String base = projectBase.replace('\\', '/');
        if (!base.endsWith("/")) base += "/";
        return file.startsWith(base) ? file.substring(base.length()) : file;
    }
}
