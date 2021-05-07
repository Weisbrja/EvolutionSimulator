plugins {
	application
	id("org.openjfx.javafxplugin") version "0.0.10"
	id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("javax.vecmath", "vecmath", "1.5.2")
	implementation("org.apache.commons", "commons-csv", "1.8")
}

application {
	mainClass.set("com.weisbrja.App")
}

javafx {
	modules("javafx.controls")
}
