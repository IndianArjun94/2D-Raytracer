plugins {
    id("java")
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

    implementation("org.jcuda:jcuda:11.8.0")
    implementation("org.jcuda:jcuda-natives:11.8.0:windows-x86_64")
}
