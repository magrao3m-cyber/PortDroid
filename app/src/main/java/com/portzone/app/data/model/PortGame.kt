package com.portzone.app.data.model

data class PortGame(
    val id: String,
    val name: String,
    val project: String,
    val projectUrl: String,
    val needsGameFiles: String,
    var driveFileId: String? = null,
    var driveFileSize: Long? = null
) {
    val isGitHub: Boolean get() = projectUrl.contains("github.com", ignoreCase = true)
    val isPlayStore: Boolean get() = projectUrl.contains("play.google.com", ignoreCase = true)
    val hasRomRequirement: Boolean
        get() = needsGameFiles.isNotBlank() &&
                !needsGameFiles.equals("NO", ignoreCase = true)
}
