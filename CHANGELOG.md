# Changelog
All notable changes to this project will be documented in this file.

## [2.0.49] - 2023-03-30
### Changed
- Add Annotation for java
- Fixed local bad uri error
- Fixed adding annotation multiple time
- Fix trying app to update nav panel on disposed instance
- Add null check
- Fixed CodeLens displaying for Java project #455
- Fixed scrollbars displaying inside React app

## [2.0.48] - 2023-03-29
### Changed
- Fix detailed error tab navigation + Fix `Expand` error button
- Bugfix/Initial state is not unknown (#433)
- bring and use icon of no_observability
- Refresh env list if it is empty on RecentAct call + Add check for the new envs
- Feature/panels of no observability are shown only for java (#443)
- Implement first time installation Wizard #369

## [2.0.47] - 2023-03-28
### Changed
- Added posthot-token-url to publish action
- Fixed & Added posthog logs level

## [2.0.46] - 2023-03-28
### Changed
- Fix null cannot be cast to non-null type kotlin.String (#434)

## [2.0.45] - 2023-03-27
### Changed
- Add general exception handler to avoid displaying of insights with corrupted data from BE
- Add null check while getting environments
- Fix memory leak

## [2.0.44] - 2023-03-26
### Changed
- Posthog plugin analytics
- Feature/SpringBoot discovery of HTTP endpoints (#426)
- Add null check while getting environments (#427)

## [2.0.43] - 2023-03-25
### Changed
- Fixed #420 - List of environments incorrectly shows data for environment

## [2.0.42] - 2023-03-23
### Changed
- Feature/Panels for No Observability (#415)
- Fix trace btn on TopUsage insight panel
- Fixed backend connection monitor (#406)

## [2.0.41-pre] - 2023-03-23
### Changed
- Feature/no data yet panel per span (#408)
- Feature/setting for Runtime Observability Backend (exporter) (#410)
- Update CodeLens automatically

## [2.0.40] - 2023-03-22
### Changed
- Quick instrumentation java code (#395)
- Decreased and fixed height for navigation panel rows (#392)
- Change background color of navigation panel dynamically (#392)
- Applied new background color for navigation panel (#392)
- Used ComboBox for environment selector (#392)
- Fixed flickering of environment selector (#392)
- Feature/Insights pending and no data yet (#396)
- Set default value for ComboBox when connection was lost and when there are no environments
- Add links to TopUsage/Bottleneck insights
- Fixed Error links (#394)
- Add Scaling Root Cause Insight
- Feature/Add SpanInfo to SpanInsights - as in backend (#403)
- Fixed UI freeze issue on SocketTimeoutException
- Fixed setting of correct env in combobox after click on goToSpan link in RecentActivity tab

## [2.0.39] - 2023-03-17
### Changed
- Added Pycharm support
- bugfix/link on span modifies the active env (#387)
- Navigation schema change - environment selection #324

## [2.0.38] - 2023-03-08
### Changed
- Repaint UI only when insights list was changed

## [2.0.37] - 2023-03-08
### Changed
- Added support for wrong methodCodeObjectId

## [2.0.36] - 2023-03-07
### Changed
- Add support for test files

## [2.0.35] - 2023-03-01
### Changed
- Applied new UI theme styles for Jetbrains

## [2.0.34] - 2023-03-01
### Changed
- Configured CodeLens for Java project to open sidepane instead of "Recent Activity"
- Improved logic to avoid random flickering of the insights view

## [2.0.33] - 2023-02-27
### Changed
- Changed the default sidepanel location from left to top right + removed Recent Activity tab name at the bottom

### Added
- #333 Dynamically change UI themes for Recent Activity

## [2.0.32] - 2023-02-26
### Changed
- Change the title of the Recent Activity panel to “Observability”

## [2.0.31] - 2023-02-23
### Added
- #318 Integrate Recent Activity view into new Digma tab at the bottom

[2.0.31]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.30...v2.0.31
[2.0.32]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.31...v2.0.32
[2.0.33]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.32...v2.0.33
[2.0.34]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.33...v2.0.34
[2.0.35]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.34...v2.0.35
[2.0.36]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.35...v2.0.36
[2.0.37]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.36...v2.0.37
[2.0.38]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.37...v2.0.38
[2.0.39]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.38...v2.0.39
[2.0.40]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.39...v2.0.40
[2.0.41-pre]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.40...v2.0.41-pre
[2.0.42]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.41-pre...v2.0.42
[2.0.43]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.42...v2.0.43
[2.0.44]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.43...v2.0.44
[2.0.45]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.44...v2.0.45
[2.0.46]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.45...v2.0.46
[2.0.47]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.46...v2.0.47
[2.0.48]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.47...v2.0.48
[2.0.49]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.48...v2.0.49
[Unreleased]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.49...HEAD