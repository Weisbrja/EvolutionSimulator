plugins {
	application
	id("org.openjfx.javafxplugin") version "0.0.9"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("javax.vecmath", "vecmath", "1.5.2")
	implementation("org.apache.commons", "commons-csv", "1.8")
}

application {
	mainModule.set("de.weisbrja")
	mainClass.set("de.weisbrja.App")
}

javafx {
	modules("javafx.controls")
}

modularity.disableEffectiveArgumentsAdjustment()