Releasing
========

 1. Create a branch from `master` in the format `release/<version_to_be_released>` Eg. `release/1.0.0`
 2. Change the `Project.version` value in `buildSrc/src/main/kotlin/Dependencies.kt` to the version number to be released.
 3. Update `CHANGELOG.md` entry with changes of the release. 
 4. Run `./gradlew clean build` to make sure project builds successfully.
 5. `git commit -am "Make release for X.Y.Z."` (where X.Y.Z is the new version).
 6. `git tag -a X.Y.Z -m "Version X.Y.Z"`
 7. `git push && git push --tags`
 8. Update `Project.version` value in `buildSrc/src/main/kotlin/Dependencies.kt` to the next SNAPSHOT version.
 9. `git commit -am "Prepare next development version."`
 10. `git push`