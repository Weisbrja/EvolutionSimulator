plugins {
	application
	id("org.openjfx.javafxplugin") version "0.0.9"
	id("org.beryx.jlink") version "2.23.4"
}

repositories {
	mavenCentral()
}

application {
	mainModule.set("de.weisbrja")
	mainClass.set("de.weisbrja.App")
}

javafx {
	modules("javafx.controls")
}

jlink {
	addOptions("--strip-debug", "--no-header-files", "--no-man-pages")
	launcher {
		name = "Evolution-Simulator"
	}
}

jpackage {
	addOptions("--win-menu", "--win-shortcut", "--win-dir-chooser")
}

dependencies {
	implementation("javax.vecmath", "vecmath", "1.5.2")
	implementation("org.apache.commons", "commons-csv", "1.8")
}

modularity.disableEffectiveArgumentsAdjustment()
