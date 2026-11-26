package com.rcmiku.music.update

import android.content.Context
import android.content.pm.ApplicationInfo
import com.pavi2410.appupdater.AndroidAssetDownloader
import com.pavi2410.appupdater.AndroidAssetInstaller
import com.pavi2410.appupdater.AppUpdater
import com.pavi2410.appupdater.GitHubUpdateSource
import com.pavi2410.appupdater.ReleaseInfo
import com.pavi2410.appupdater.UpdateSource

private const val OWNER = "xu-bu"
private const val REPO = "JetMelo"
private const val TAG_PREFIX = "build-"

/**
 * Releases are tagged "build-N" (N = CI run number), which the library's version comparison cannot
 * read. Map the tag to "1.0.N", the same format as the app's versionName (see app/build.gradle.kts).
 */
private class BuildTagSource(private val delegate: UpdateSource) : UpdateSource {
    override suspend fun fetchReleases(): List<ReleaseInfo> =
        delegate.fetchReleases().mapNotNull { release ->
            val number = release.tagName.removePrefix(TAG_PREFIX).toIntOrNull()
                ?: return@mapNotNull null
            release.copy(version = "1.0.$number")
        }
}

fun createAppUpdater(context: Context): AppUpdater {
    val currentVersion = context.packageManager
        .getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    return AppUpdater(
        currentVersion = currentVersion,
        source = BuildTagSource(GitHubUpdateSource(owner = OWNER, repo = REPO)),
        downloader = AndroidAssetDownloader(context),
        installer = AndroidAssetInstaller(context, "${context.packageName}.fileprovider")
    )
}

/** Local debug builds carry build number 0, so they would always report an update. */
fun Context.isDebuggableBuild(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
