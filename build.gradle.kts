import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.lhstack"
version = "0.0.2"


repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
}

intellij {
    version.set("2022.3")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("com.intellij.java", "org.jetbrains.plugins.yaml", "org.intellij.groovy","org.jetbrains.kotlin"))
}

dependencies {
    implementation("cn.hutool:hutool-poi:5.8.36")
    implementation("org.apache.poi:poi-ooxml:5.0.0")
    implementation("org.springframework:spring-expression:5.3.39")
    implementation(files("C:/Users/1/.jtools/sdk/sdk.jar"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }
    withType<JavaExec> {
        jvmArgs("-Dfile.encoding=UTF-8")
    }

    withType<Jar>(){
        archiveBaseName = "jtools-text-convert"
    }

    withType<ShadowJar> {
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer::class.java)
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.XmlAppendingTransformer::class.java)
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.XmlAppendingTransformer::class.java)
        exclude("META-INF/MANIFEST.MF","META-INF/*.SF","META-INF/*.DSA")
        dependencies {
            exclude(dependency("com.jetbrains.*:.*:.*"))
            exclude(dependency("org.jetbrains.*:.*:.*"))
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }

}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}