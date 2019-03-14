# Changelog

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]

## [0.3.0] - 2019-03-14
### Added
- Added SNAPSHOT releases to JFrog
- Added handler resolution section to README
- Support for simple regex path matching

### Fixed
- Ensure that routes are always selected in order of registration

## [0.2.0] - 2019-01-22
- Improved path matching.
  - Route pattern with/without trailing slash now matches URI with/without trailing slash.
  - Named/nameless parameters now ignore empty path segments at the end of a URI. 
  However, empty segments occurring before non-empty ones are still processed accordingly
- Changed visibility of `MatchResult` to `internal`.

## [0.1.0] - 2019-01-17
### Added
- Implemented deep link parser.
- Platform agnostic URI implementation.
- Ignore query keys clashing with predefined path params.
- Support for treating host as path.
- Added code sample to readme.
- Set up build automation.
- Added transformation extensions for DeepLinkUri to/from `android.net.Uri`.
- Implemented support for nameless path segment matching.
- Added unit test for `DeepLinkUri`.

[Unreleased]: https://github.com/hellofresh/android-deeplink/compare/0.3.0...HEAD
[0.3.0]: https://github.com/hellofresh/android-deeplink/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/hellofresh/android-deeplink/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/hellofresh/android-deeplink/compare/2a89d70648ee809bac78a3b768fe664d3f04aad8...0.1.0
