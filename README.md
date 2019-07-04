# nvd-container-plugin

The most important part of this project is inside `buildSrc` directory - a Gradle plugin, which uses a containerized version of NVD database for Dependency Checking.

The _main_ project is not that important at all. It just showcases that if you run
```
./gradlew build
```
[OWASP Dependency Check](https://github.com/jeremylong/DependencyCheck) is executed and produces correct results (in `build/reports`).

Where to get the image
---
The National Vulnerability Database image can be built using a Gradle script from [this repository](https://github.com/zoomint/nvd-container-gradle). 