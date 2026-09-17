package com.donutsmp.usbdeck.homebrew

enum class HomebrewKind {
    Documentation,
    OpenSourceTool,
    HomebrewApplication,
    SystemModification
}

data class HomebrewItem(
    val id: String,
    val name: String,
    val summary: String,
    val kind: HomebrewKind,
    val sourceLabel: String,
    val homepage: String,
    val downloadUrl: String?,
    val downloadFileName: String?,
    val expectedSha256: String?,
    val notes: String
)

object HomebrewCatalog {
    val items: List<HomebrewItem> = listOf(
        HomebrewItem(
            id = "psdevwiki",
            name = "PS3 Developer Wiki",
            summary = "Community documentation for PS3 hardware, software, and homebrew development.",
            kind = HomebrewKind.Documentation,
            sourceLabel = "psdevwiki.com",
            homepage = "https://www.psdevwiki.com/ps3/",
            downloadUrl = null,
            downloadFileName = null,
            expectedSha256 = null,
            notes = "Read-only documentation. Open in a browser. No binary is bundled."
        ),
        HomebrewItem(
            id = "psl1ght",
            name = "PSL1GHT",
            summary = "Open-source PS3 homebrew SDK used to build legitimate homebrew applications.",
            kind = HomebrewKind.OpenSourceTool,
            sourceLabel = "GitHub · ps3dev/PSL1GHT",
            homepage = "https://github.com/ps3dev/PSL1GHT",
            downloadUrl = "https://github.com/ps3dev/PSL1GHT/archive/refs/heads/master.zip",
            downloadFileName = "PSL1GHT-master.zip",
            expectedSha256 = null,
            notes = "Source snapshot from the official GitHub repository. Hash is not pinned because the branch moves."
        ),
        HomebrewItem(
            id = "ps3toolchain",
            name = "ps3toolchain",
            summary = "Open-source toolchain scripts for building PS3 homebrew from source.",
            kind = HomebrewKind.OpenSourceTool,
            sourceLabel = "GitHub · ps3dev/ps3toolchain",
            homepage = "https://github.com/ps3dev/ps3toolchain",
            downloadUrl = "https://github.com/ps3dev/ps3toolchain/archive/refs/heads/master.zip",
            downloadFileName = "ps3toolchain-master.zip",
            expectedSha256 = null,
            notes = "Source only. It is not firmware, CFW, or an exploit payload."
        ),
        HomebrewItem(
            id = "ps3libraries",
            name = "ps3libraries",
            summary = "Open-source port libraries used with the ps3dev toolchain.",
            kind = HomebrewKind.OpenSourceTool,
            sourceLabel = "GitHub · ps3dev/ps3libraries",
            homepage = "https://github.com/ps3dev/ps3libraries",
            downloadUrl = "https://github.com/ps3dev/ps3libraries/archive/refs/heads/master.zip",
            downloadFileName = "ps3libraries-master.zip",
            expectedSha256 = null,
            notes = "Source snapshot. Verify the GitHub repository before building."
        ),
        HomebrewItem(
            id = "ps3dev-hello",
            name = "ps3dev organization",
            summary = "Umbrella organization for maintained open-source PS3 development tools.",
            kind = HomebrewKind.OpenSourceTool,
            sourceLabel = "GitHub · ps3dev",
            homepage = "https://github.com/ps3dev",
            downloadUrl = null,
            downloadFileName = null,
            expectedSha256 = null,
            notes = "Browse the organization and pick a specific project yourself."
        )
    )

    fun kindLabel(kind: HomebrewKind): String = when (kind) {
        HomebrewKind.Documentation -> "Documentation"
        HomebrewKind.OpenSourceTool -> "Open-source tool"
        HomebrewKind.HomebrewApplication -> "Homebrew application"
        HomebrewKind.SystemModification -> "System modification software"
    }
}
