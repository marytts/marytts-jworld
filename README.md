[![Build Status](https://travis-ci.org/marytts/marytts-jworld.svg?branch=master)](https://travis-ci.org/marytts/marytts-jworld)

# marytts-jworld

This artifact contains the helpers as well as a module to integrate WORLD vocoding into the MaryTTS synthesis pipeline.

## Gradle dependency

```gradle
repositories {
    maven {
        url 'https://oss.jfrog.org/artifactory/oss-release-local'
    }
}

dependencies {
    compile group: 'de.dfki.mary', name: 'marytts-jworld', version: '0.1'
}
```
