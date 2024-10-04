import java.net.URI
import java.net.URL
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.io.FileWriter
import java.io.FileOutputStream
import java.net.HttpURLConnection
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
val pluginVersion = properties("pluginVersion")
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
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044",
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
           val pluginZip = file("build/distributions/OCIPluginForIntelliJ_stamped.zip")
           var dest = repoUri+"/com/oracle/oci/intellij/plugin/i_builds/OCIPluginForIntelliJ"
           dest += "-"+pluginVersion+"-"
           val jobStartTime = System.getenv("CI_JOB_STARTED_AT")
           var commitBranch = System.getenv("CI_COMMIT_BRANCH")
           var mergeSourceBranch = System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME")
           val mergeRequestNumber = System.getenv("CI_MERGE_REQUEST_IID")
           // MR is merged.
           if (mergeRequestNumber != null) {
               dest+="MR_"+mergeRequestNumber
               if (mergeSourceBranch != null) {
                  mergeSourceBranch = mergeSourceBranch.replace('/', '_')
                  mergeSourceBranch = mergeSourceBranch.replace('-', '_')
                  dest+=mergeSourceBranch + ".zip"
               
               }
           }
           // MR in progress
           else if (commitBranch != null) {
               commitBranch = commitBranch.replace('/', '_')
               commitBranch = commitBranch.replace('-', '_')
               dest += commitBranch + ".zip"
           }
           // any other build type (TBD)
           else if (jobStartTime != null) {
	           dest += jobStartTime + ".zip"
	       }
	       // generic build with no info.
	       else {
	           dest += "SNAPSHOT.zip"
	       }
           val url = URL(dest)
           val connection = url.openConnection() as HttpURLConnection
           val upass = usernameStr+":"+passwordStr
           val upassBytes = upass.toByteArray()
           val encodedAuth = Base64.getEncoder().encodeToString(upassBytes)

           connection.setRequestProperty("Authorization", "Basic "+encodedAuth)

           connection.doOutput = true
           connection.requestMethod = "PUT"
           connection.setRequestProperty("Content-Type", "application/octet-stream")
           val fileLen = pluginZip.length()
           connection.setRequestProperty("Content-Length", fileLen.toString())
           connection.setRequestProperty("User-Agent", "OCI-Gradle-FileUpload")

           connection.outputStream.use{ output ->
               Files.copy(pluginZip.toPath(), output)
               output.flush()
               output.close()
           }
 
           val respCode = connection.responseCode
           System.out.println(respCode)
      }
}

tasks.named("publish") {
//    dependsOn("updateDistroZipWithStamp")
    dependsOn("uploadPluginZip")
}

tasks.register("createBuildStamp") {
    doLast() {
       val CI_COMMIT_SHA = System.getenv("CI_COMMIT_SHA")
       val CI_COMMIT_TIMESTAMP = System.getenv("CI_COMMIT_TIMESTAMP")
       //System.getenv("I_DEFAULT_BRANCH
       //System.getenv("CI_JOB_ID}
       val CI_COMMIT_BRANCH = System.getenv("CI_COMMIT_BRANCH")
       //System.getenv("GITLAB_CI}
       //System.getenv("CI_JOB_STARTED_AT}
       //System.getenv("CI_MERGE_REQUEST_ID}
       //System.getenv("CI_MERGE_REQUEST_IID}
       //System.getenv("CI_PIPELINE_SOURCE}
       //System.getenv("CI_MERGE_REQUEST_APPROVED}
       //System.getenv("CI_MERGE_REQUEST_DIFF_ID}
       //System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME})
       val path = Path.of(project.projectDir.getAbsolutePath()+"/build/distributions/")
       System.out.println(Files.createDirectories(path))
       val writer = FileWriter("build/distributions/build_stamp.txt")
		try {
			writer.write(String.format("CI_COMMIT_SHA=%s\n", CI_COMMIT_SHA))
			writer.write(String.format("CI_COMMIT_TIMESTAMP=%s\n", CI_COMMIT_TIMESTAMP))
			writer.write(String.format("CI_COMMIT_BRANCH=%s\n", CI_COMMIT_BRANCH))
			writer.flush()
		}
		finally {
		    if (writer != null) {
		        writer.close();
		    }
		}
    }
}

tasks.named("assemble") {
    dependsOn("updateDistroZipWithStamp")
}

tasks.register("updateDistroZipWithStamp", Zip::class) {
	archiveFileName.set("OCIPluginForIntelliJ_stamped.zip")
	destinationDirectory.set(file("build/distributions"))
	
	from(zipTree("build/distributions/OCIPluginForIntelliJ.zip"))
	
	from("build/distributions/build_stamp.txt") {
	    into("/")
	}
	dependsOn("createBuildStamp")
	dependsOn("buildPlugin")
}

