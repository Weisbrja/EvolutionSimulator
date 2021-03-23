plugins {
	application
	id("org.openjfx.javafxplugin") version "0.0.9"
	id("org.beryx.jlink") version "2.23.4"
}

repositories {
	mavenCentral()
}

application {
	mainClass.set("de.weisbrja.App")
}

javafx {
	version = "16"
	modules("javafx.controls")
}

jlink {
	addOptions("--strip-debug", "--no-header-files", "--no-man-pages")
	launcher {
		name = "launch"
	}
}

dependencies {
	implementation("javax.vecmath", "vecmath", "1.5.2")
	implementation("org.apache.commons", "commons-csv", "1.8")
}

modularity.disableEffectiveArgumentsAdjustment()