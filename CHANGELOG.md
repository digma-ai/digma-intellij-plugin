# Changelog
All notable changes to this project will be documented in this file.

## [2.0.61] - 2023-04-13
### Changed
- Show `Not Reached` only for instrumented endpoints/spans
- Fixed TopUsages insight flows layouts
- Ensure read action issue 526 (#534)

## [2.0.60] - 2023-04-13
### Changed
- Show 4 Top Usage insights per page
- Show arrow buttons only if content is bigger then environment panel Close #50 + Fix scroll
- Added server version to analytics

## [2.0.59] - 2023-04-11
### Changed
- changed org key in plugin xml (#524)

## [2.0.58] - 2023-04-11
### Changed
- Added plugin loaded analytics event
- Added link to go back to installation wizard
- Add pagination to TopUsage panel
- Fixed usageStatus must not be null #508
- Applied new style for Recent activity environments selector
- Add `Never Reached` code lens for code objects with no data
- Show `Runtime data` on any method if we have any non-critical insights
- Support showing wizard in different versions for Pycharm, Rider #519

## [2.0.57] - 2023-04-09
### Changed
- Disable env icon if current method does not have insights #420
- UI update: light theme for wizard + new animations
- Fixed Rider is not responding #311
- Feature/refresh faster when clicking recent activity (#495)
- Users can now navigate back to steps they skipped over in the onbo… (#506)
- Users can now navigate back to steps they skipped over in the onboarding wizard. 2. Added a direct link to the Docker app from the 'connection error' page 
- Added better styling for navigation into steps in the wizard 
- Switched order of empty states to ensure we show the no observability panel even when we don't have insights data 
- improved previous commit 
- fixed some statuses not being shown

## [2.0.56] - 2023-04-06
### Changed
- Added tmp posthog token to code

## [2.0.55] - 2023-04-05
### Changed
- Bug fix/do not override OTEL service name if already defined (#479)
- Pass events from react installation wizard to Posthog
- Recent activity should not show 'Trace' button if there is no settings for Jaeger#484
- Feature/refresh refactoring - refresh file even if scope is not method (#494)
- Integrate Installation Wizard changes with new dark theme + toggle button #497
- Fixed NPE for getCodeObjectsWithInsightsStatus
- Removed redundant notifyModelChangedAndUpdateUi call (duplicated call)

## [2.0.54] - 2023-04-04
### Changed
- Add analytics for the 'observablity' toggle button in IntelliJ
- Bug fix/make a copy of env map (#488)
- Update the icon for the Observability panel (use telescope icon) #485
- Feature/RunConfig - simply modify the (System) properties

## [2.0.53] - 2023-04-03
### Changed
- Fixed insight item description word wrapping (#457)
- Feature/settings at application level instead of project level (#475)
- Bug fix/range check (#476)
- Bug fix/avoid nulls and instead return empty list (#477)
- Bug fix/handle possible null methodId (#478)

## [2.0.50] - 2023-03-31
### Changed
- Fix showing 'Loading...' when digma has no envs yet
- Save Installation Wizard state globally on application level #459
- If the server is already connected, don't show the first page of the wizard, only show the second page.#462
- #36 Update button styles in Installation Wizard

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
[2.0.50]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.49...v2.0.50
[2.0.53]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.50...v2.0.53
[2.0.54]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.53...v2.0.54
[2.0.55]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.54...v2.0.55
[2.0.56]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.55...v2.0.56
[2.0.57]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.56...v2.0.57
[2.0.58]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.57...v2.0.58
[2.0.59]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.58...v2.0.59
[2.0.60]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.59...v2.0.60
[2.0.61]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.60...v2.0.61
[Unreleased]: https://github.com/digma-ai/digma-intellij-plugin/compare/v2.0.61...HEAD