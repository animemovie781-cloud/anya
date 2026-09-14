package com.amaya.intelligence.appbuilder.engine

import java.io.File

/**
 * Handles package name conversion and path generation.
 */
object PackagePathGenerator {

    private val RESERVED_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null", "val", "var", "fun", "when", "is", "in", "as"
    )

    private val IDENTIFIER_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

    /**
     * Converts a package name like `com.example.myapp` into a relative path like `com/example/myapp`.
     */
    fun toPackagePath(packageName: String): String {
        return packageName.trim().replace('.', '/')
    }

    /**
     * Computes the complete relative source directory for a package.
     * E.g. ("app/src/main/kotlin", "com.example.myapp") -> "app/src/main/kotlin/com/example/myapp"
     */
    fun toSourceDirPath(sourceRoot: String, packageName: String): String {
        val cleanRoot = sourceRoot.trimEnd('/', '\\')
        val pkgPath = toPackagePath(packageName)
        return if (pkgPath.isEmpty()) cleanRoot else "$cleanRoot/$pkgPath"
    }

    /**
     * Computes the full path to a source file given root, package, and filename.
     */
    fun toSourceFilePath(sourceRoot: String, packageName: String, fileName: String): String {
        val dir = toSourceDirPath(sourceRoot, packageName)
        return "$dir/$fileName"
    }

    /**
     * Validates whether a package name is structurally valid.
     * Must have at least two segments, each segment must be a valid identifier and not a reserved word.
     */
    fun validate(packageName: String): ValidationResult {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Package name cannot be empty")
        }

        val segments = trimmed.split('.')
        if (segments.size < 2) {
            return ValidationResult.Invalid("Package name must contain at least two segments (e.g. com.example.app)")
        }

        for (segment in segments) {
            if (segment.isEmpty()) {
                return ValidationResult.Invalid("Package name contains empty segment between dots")
            }
            if (!IDENTIFIER_REGEX.matches(segment)) {
                return ValidationResult.Invalid("Invalid package segment '$segment': must begin with letter or underscore and contain only alphanumeric characters or underscores")
            }
            if (RESERVED_KEYWORDS.contains(segment.lowercase())) {
                return ValidationResult.Invalid("Package segment '$segment' is a reserved keyword")
            }
        }

        return ValidationResult.Valid
    }
}
