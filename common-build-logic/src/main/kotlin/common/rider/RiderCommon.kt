package common.rider

import common.BuildProfiles


fun rdGenVersion(profile: String): String {

    val profileToUse: BuildProfiles.Profiles = when (profile) {
        "lowest" -> BuildProfiles.Profiles.p231
        "latest" -> BuildProfiles.Profiles.p241
        "eap" -> BuildProfiles.Profiles.p242
        else -> BuildProfiles.Profiles.valueOf(profile)
    }

    return rdGenVersionByProfile(profileToUse)

}


fun rdGenVersionByProfile(profile: BuildProfiles.Profiles): String {
    return when (profile) {
        BuildProfiles.Profiles.p231 -> "2023.2.0"
        BuildProfiles.Profiles.p232 -> "2023.2.2"
        BuildProfiles.Profiles.p233 -> "2023.3.2"
        BuildProfiles.Profiles.p241 -> "2023.3.2"
        BuildProfiles.Profiles.p242 -> "2024.1.1"
    }
}