plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.arjun.justwalkforward"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jcuda.org/maven")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.jcuda:jcuda:11.6.1") {
        isTransitive = false
    }
    implementation("org.jcuda:jcuda-natives:11.6.1:windows-x86_64")
}

tasks {
    // Replace "your.main.Class" with your main class (with package name)
    jar {
        manifest {
            attributes["Main-Class"] = "net.arjun.justwalkforward.Main"
        }
    }

    shadowJar {
        archiveBaseName.set("raytracer")
        archiveVersion.set("1.0")
        archiveClassifier.set("") // so it's just YourAppName.jar
    }
}