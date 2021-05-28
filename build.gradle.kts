plugins {
    java
}

group = "MopeSWTP"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
    compile("org.eclipse.lsp4j:org.eclipse.lsp4j:0.12.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}