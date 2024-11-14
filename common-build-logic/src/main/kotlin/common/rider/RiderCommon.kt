package common.rider

import common.BuildProfiles


fun rdGenVersion(profile: String): String {

    val profileToUse: BuildProfiles.Profile = when (profile) {
        "lowest" -> BuildProfiles.Profile.p231
        "latest" -> BuildProfiles.Profile.p242
        "eap" -> BuildProfiles.Profile.p243
        else -> BuildProfiles.Profile.valueOf(profile)
    }

    return rdGenVersionByProfile(profileToUse)

}


fun rdGenVersionByProfile(profile: BuildProfiles.Profile): String {
    return when (profile) {
        BuildProfiles.Profile.p231 -> "2023.2.0"
        BuildProfiles.Profile.p232 -> "2023.2.2"
        BuildProfiles.Profile.p233 -> "2023.3.2"
        BuildProfiles.Profile.p241 -> "2023.3.2"
        BuildProfiles.Profile.p242 -> "2024.1.1"
        BuildProfiles.Profile.p243 -> "2024.3.1"
        BuildProfiles.Profile.p251 -> "2024.3.1"
    }
}
