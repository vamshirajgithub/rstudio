## RStudio 2023.08.0 "Desert Sunflower" Release Notes

### New
- Prometheus metrics are available in Posit Workbench (rstudio-pro:#3273)

### Fixed
- Fixed issue where Electron menubar commands are not disabled when modals are displayed (#12972)
- Fixed bug causing invalid/empty `cacheKey` error when accessing a dataframe variable (#13188)
- Fixed bug preventing dataframe column navigation past the default number of display columns (#13220)
- Fixed intermittent rsession crash when the linux nscd service was enabled (rstudio-pro:#4648)
- Fixed bug when resuming session not restoring current working directory for Terminal pane (rstudio-pro:#4027)

### Performance
- Improved performance of group membership tests (rstudio-pro:#4643)
- Increased read buffer size for rserver proxy (rstudio-pro:#4764)

### Accessibility Improvements
-

