package common.rider

import common.BuildProfiles


fun rdGenVersion(profile: String): String {

    val profileToUse: BuildProfiles.Profile = when (profile) {
        "lowest" -> BuildProfiles.Profile.p241
        "latest" -> BuildProfiles.Profile.p251
        "eap" -> BuildProfiles.Profile.p252
        else -> BuildProfiles.Profile.valueOf(profile)
    }

    return rdGenVersionByProfile(profileToUse)

}


fun rdGenVersionByProfile(profile: BuildProfiles.Profile): String {
    return when (profile) {
        BuildProfiles.Profile.p241 -> "2023.3.2"
        BuildProfiles.Profile.p242 -> "2024.1.1"
        BuildProfiles.Profile.p243 -> "2024.3.1"
        BuildProfiles.Profile.p251 -> "2025.1.1"
        BuildProfiles.Profile.p252 -> "2025.1.1"
    }
}
