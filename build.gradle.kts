import java.net.URI
import java.net.URL
import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.nio.file.Files
import java.util.Base64

fun properties(key: String) = project.findProperty(key).toString()

val ADMIN_IDE_TEST_KEY_PEM = System.getenv("ADMIN_IDE_INTEGRATION_PEM")
val IDE_INTEGRATION_KEY_PEM = System.getenv("IDE_INTEGRATION_TEST_PEM")
val IDE_PLUGIN_JUNIT_PEM = System.getenv("IDE_PLUGIN_JUNIT_PEM")


if (IDE_INTEGRATION_KEY_PEM == null || ADMIN_IDE_TEST_KEY_PEM == null || IDE_PLUGIN_JUNIT_PEM == null) {
    System.err.println("Test Key Url's not found");
}

plugins {
    idea
    // Java support
    id("java")
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.13.1"
    id("maven-publish")
    id("distribution")

}

val group = properties("pluginGroup")
val artifactId = "oci.intellij.plugin"
val version = properties("pluginVersion")
val sinceBuildVersion = properties("pluginSinceBuild")
val pluginArtifactId = "oci.intellij.plugin"

// Configure project's dependencies
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-core:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-database:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-identitydataplane:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-resourcemanager:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-vault:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-keymanagement:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-devops:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-certificatesmanagement:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-dns:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.oracle.oci.sdk:oci-java-sdk-artifacts:3.24.0") {
        exclude(group="org.slf4j", module="slf4j-api")
    }

    implementation("org.yaml:snakeyaml:2.2") {
        exclude(group="org.slf4j", module="slf4j-api")
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0") // Use the latest version
    
    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("jakarta.json:jakarta.json-api:2.1.2")
    testImplementation("org.eclipse.parsson:parsson:1.1.5")
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    //downloadSources.set(!isCI)
    updateSinceUntilBuild.set(false)
    //instrumentCode.set(false)
    //ideaDependencyCachePath.set(dependencyCachePath)
    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    //sandboxDir.set("$buildDir/$baseIDE-sandbox-$platformVersion")
}


tasks {
    runIde {
        systemProperties["idea.auto.reload.plugins"] = true
        systemProperties["idea.log.debug.categories"] = true
        jvmArgs = listOf(
            "-Xms512m",
            "-Xmx2048m",
            "-ea",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044",
        )
    }

    patchPluginXml {
        version.set("${project.version}")
        sinceBuild.set(sinceBuildVersion)
    } 

    runPluginVerifier {
    }
}

distributions {
  main {
    distributionBaseName.set("OCIPluginForIntelliJ.zip")
  }
}


val repoUri = System.getenv("JDBC_DEV_LOCAL_URL")  //"https://artifacthub-phx.oci.oraclecorp.com/jdbc-dev-local"
val usernameStr = System.getenv("JDBC_DEV_LOCAL_USERNAME")
val passwordStr = System.getenv("JDBC_DEV_LOCAL_APIKEY")

publishing {
    publications {
      create<MavenPublication>("mavenJava") {
                groupId = "com.oracle" 
                artifactId = pluginArtifactId
 		version = "1.1.0-SNAPSHOT"
                from(components["java"])
            }
    }


    repositories {
        maven {
            credentials {
                username = usernameStr
                password = passwordStr
            }
            url = URI(repoUri)
        }
     }
}

tasks.register("downloadPemFiles") {
    val PEM_FILE_PATH_VARS = listOf("ADMIN_IDE_INTEGRATION_TEST_USER_KEY_PATH", "IDE_INTEGRATION_TEST_USER_KEY_PATH", "IDE_PLUGIN_JUNIT_05_26_22_KEY_PATH")

    // order matters
    val PEM_KEY_FILES = listOf("ADMIN_IDE_INTEGRATION_PEM", "IDE_INTEGRATION_TEST_PEM", "IDE_PLUGIN_JUNIT_PEM")
    doLast() {
        for (i in PEM_FILE_PATH_VARS.indices) {
            val localPath = System.getenv(PEM_FILE_PATH_VARS[i])
            System.out.println("Localpath: "+localPath)
            val pem_file_var = PEM_KEY_FILES[i]
            System.out.println(pem_file_var)
            val file = File(localPath)
            System.out.println("Writing pemFile: "+file.toString());
            val pemFile = System.getenv(pem_file_var);
            val outFile = FileWriter(file);
            outFile.write(pemFile);
            outFile.flush();
            outFile.close();
        }
    }
}

tasks.named("test") {
    dependsOn("downloadPemFiles")
}

tasks.register("uploadPluginZip") {
      doLast() {
           val pluginZip = file("build/distributions/OCIPluginForIntelliJ.zip")
           val dest = repoUri+"/com/oracle/oci/intellij/plugin/i_builds/OCIPluginForIntelliJ-1.0.2-SNAPSHOT.zip"
           val url = URL(dest)
           val connection = url.openConnection() as HttpURLConnection
           val upass = usernameStr+":"+passwordStr
           val upassBytes = upass.toByteArray()
           val encodedAuth = Base64.getEncoder().encodeToString(upassBytes)

           connection.setRequestProperty("Authorization", "Basic "+encodedAuth)

           connection.doOutput = true
           connection.requestMethod = "PUT"
           connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=fileboundary")
           connection.setRequestProperty("User-Agent", "OCI-Gradle-FileUpload")

	   val boundary = "--fileboundary\r\n"
           connection.outputStream.use{ output ->
               // Write file content
               output.write(boundary.toByteArray())
               output.write("Content-Disposition: form-data; name=\"file\"; filename=\"${pluginZip.name}\"\r\n\r\n".toByteArray())
               Files.copy(pluginZip.toPath(), output)
               output.write("\r\n--fileboundary--\r\n".toByteArray())
           }
 
           val respCode = connection.responseCode
           System.out.println(respCode)
      }
}

tasks.named("publish") {
    dependsOn("uploadPluginZip")
}
