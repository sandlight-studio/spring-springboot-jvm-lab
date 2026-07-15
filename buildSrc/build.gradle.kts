import org.gradle.plugin.use.PluginDependency

plugins {
    // Enables writing convention plugins as precompiled *.gradle.kts scripts.
    `kotlin-dsl`
}

// Convention plugins apply third-party plugins by id (versionless), so those
// plugins must be on buildSrc's classpath. This helper turns a catalog plugin
// alias into its Gradle "plugin marker" artifact, keeping versions in
// gradle/libs.versions.toml as the single source of truth.
fun pluginArtifact(provider: Provider<PluginDependency>): Provider<String> =
    provider.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

dependencies {
    implementation(pluginArtifact(libs.plugins.kotlin.jvm))
    implementation(pluginArtifact(libs.plugins.kotlin.spring))
    implementation(pluginArtifact(libs.plugins.kotlin.jpa))
    implementation(pluginArtifact(libs.plugins.spring.boot))
    implementation(pluginArtifact(libs.plugins.spring.dependency.management))
}
