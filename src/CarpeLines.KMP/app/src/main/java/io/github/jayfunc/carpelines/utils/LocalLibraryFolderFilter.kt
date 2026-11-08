package io.github.jayfunc.carpelines.utils

/**
 * The user-imported folders used for the deep local search.
 *
 * Importing a folder does not change which songs exist in the library; it tells the scanner
 * where to read embedded lyric tags, embedded album art and sibling `.lrc` files, and lets the
 * local music page report what those folders contain. Paths are compared after trimming the
 * trailing separator and ignoring case, because MediaStore and the folder picker can disagree
 * on both.
 */
object LocalLibraryFolderFilter {

    fun normalize(path: String): String = path.trim().trimEnd('/')

    fun isUnder(filePath: String?, root: String): Boolean {
        if (filePath.isNullOrBlank()) return false
        val normalizedRoot = normalize(root)
        if (normalizedRoot.isEmpty()) return false
        val normalizedPath = normalize(filePath)
        return normalizedPath.equals(normalizedRoot, ignoreCase = true) ||
            normalizedPath.startsWith("$normalizedRoot/", ignoreCase = true)
    }

    /** True when [filePath] falls inside one of the imported search folders. */
    fun isInside(filePath: String?, roots: Set<String>): Boolean {
        if (roots.isEmpty()) return false
        return roots.any { isUnder(filePath, it) }
    }
}
