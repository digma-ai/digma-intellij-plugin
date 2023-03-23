# Changelog
All notable changes to this project will be documented in this file.

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
[2.0.39]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.39...v2.0.40
[Unreleased]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.40...HEAD