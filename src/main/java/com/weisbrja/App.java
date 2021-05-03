package com.weisbrja;

import com.weisbrja.data.CSVConverterSpecies;
import com.weisbrja.event.EventBus;
import com.weisbrja.population.Population;
import com.weisbrja.simulation.Boundary2d;
import com.weisbrja.simulation.Boundary4d;
import com.weisbrja.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

	private static final long randomSeed = 987654351976349L;
	private static final int threadCount = 8;
	private static final boolean saveData = false;
	private static final boolean stopCreaturesWhenOnGround = true;

	private static final String filenameFitness = "fitness_" + randomSeed + ".csv";
	private static final String filenameMutationRate = "mutation_rate_" + randomSeed + ".csv";
	private static final String filenameSpecies = "species_" + randomSeed + ".csv";

	private AppContext appContext;

	private Population population;

	public static void main(String[] args) {
		launch();
	}

	@Override
	public void init() {
		// TODO: 12/13/20 Implement UI for changing simulation value ranges
		// TODO: 2/3/21 Implement UI for changing random seed at runtime
		EventBus eventBus = new EventBus();

		RandomNumberGenerator randomNumberGenerator = new RandomNumberGenerator(randomSeed);

		int simulationCycleCount = 1500;

		double gravityY = 2d;
		double groundDamping = 0.8d;
		double airFriction = 0.8d;

		Boundary4d circlePositionBoundaries = new Boundary4d(-80d, 80d, -160d, 0d);
		Boundary2d circleRadiusBoundaries = new Boundary2d(10d, 20d);

		double maxMuscleForce = 10d;
		Boundary2d muscleStrengthBoundaries = new Boundary2d(10d, 30d);
		Boundary2d muscleLengthPhasesBoundaries = new Boundary2d(50d, 120d);
		Boundary2d muscleClockSpeedBoundaries = new Boundary2d(0d, 0.05d);

		Boundary2d creatureMutationRateBoundaries = new Boundary2d(0.001d, 0.2d);
		Boundary2d creatureStructuralMutationRateBoundaries = new Boundary2d(0.001d, 0.1d);

		appContext = new AppContext(
				eventBus,

				randomNumberGenerator,

				stopCreaturesWhenOnGround,

				gravityY,
				groundDamping,
				airFriction,

				circlePositionBoundaries,
				circleRadiusBoundaries,

				maxMuscleForce,
				muscleStrengthBoundaries,
				muscleLengthPhasesBoundaries,
				muscleClockSpeedBoundaries,

				creatureMutationRateBoundaries,
				creatureStructuralMutationRateBoundaries
		);

		population = new Population(appContext, saveData, filenameFitness, filenameMutationRate, filenameSpecies, threadCount, simulationCycleCount);
	}

	@Override
	public void start(Stage stage) {
		MainView mainView = new MainView(appContext);

		// initialize the scene
		Scene scene = new Scene(mainView, 1920d, 1080d);
		scene.getStylesheets().add("bootstrap3.css");
		stage.setScene(scene);
		stage.setOnCloseRequest(windowEvent -> {
			if (saveData)
				try {
					new CSVConverterSpecies().convert(filenameSpecies, "converted_" + filenameSpecies);
				} catch (IOException e) {
					e.printStackTrace();
				}
			System.exit(0);
		});

		new Thread(() -> {
			population.initialize(10000);
			population.startSimulating();
		}).start();

		stage.show();
	}
}