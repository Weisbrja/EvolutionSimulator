package com.weisbrja.view;

import com.weisbrja.AppContext;
import com.weisbrja.simulation.GenerationDoneEvent;
import com.weisbrja.simulation.SimulationModeChangedEvent;
import com.weisbrja.simulation.SimulatorDoneEvent;
import com.weisbrja.simulation.SimulatorStartedEvent;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class MainView extends Pane {

	private boolean simulateGraphically;
	private int creatureCount;

	public MainView(AppContext appContext) {
		Font font = Font.getDefault();

		SimulationCanvas simulationCanvas = new SimulationCanvas(appContext);

		simulateGraphically = true;
		String simulationModeString = "Turn graphical ";
		Button simulationModeButton = new Button(simulationModeString + "off");
		simulationModeButton.setDefaultButton(true);
		simulationModeButton.setFont(font);
		simulationModeButton.setOnAction(actionEvent -> {
			simulationModeButton.setDisable(true);

			// flip the simulate graphically variable
			simulateGraphically = !simulateGraphically;

			simulationModeButton.setText(simulationModeString + (simulateGraphically ? "off" : "on"));

			// change the simulation mode if simulating graphically
			if (!simulateGraphically)
				appContext.getEventBus().emit(new SimulationModeChangedEvent(simulateGraphically));
		});
		appContext.getEventBus().emit(new SimulationModeChangedEvent(simulateGraphically));

		String creatureCountString = "Creature: ";
		Label creatureCountLabel = new Label(creatureCountString + 0);
		creatureCountLabel.setFont(font);

		String speciesString = "Species: ";
		Label speciesLabel = new Label(speciesString);
		speciesLabel.setFont(font);

		String mutationRateString = "Mutation rate: ";
		Label mutationRateLabel = new Label(mutationRateString);
		mutationRateLabel.setFont(font);

		String structuralMutationRateString = "Structural mutation rate: ";
		Label structuralMutationRateLabel = new Label(structuralMutationRateString);
		structuralMutationRateLabel.setFont(font);

		String generationCountString = "Generation: ";
		Label generationCountLabel = new Label(generationCountString + 0);
		generationCountLabel.setFont(font);

		String bestFitnessString = "Best fitness: ";
		Label bestFitnessLabel = new Label(bestFitnessString + Double.NEGATIVE_INFINITY);
		bestFitnessLabel.setFont(font);

		String medianFitnessString = "Median fitness: ";
		Label medianFitnessLabel = new Label(medianFitnessString + Double.NEGATIVE_INFINITY);
		medianFitnessLabel.setFont(font);

		VBox vBox = new VBox();
		vBox.setSpacing(10d);
		vBox.setPadding(new Insets(10d, 10d, 10d, 10d));
		vBox.getChildren().addAll(
				simulationModeButton,
				creatureCountLabel,
				speciesLabel,
				mutationRateLabel,
				structuralMutationRateLabel,
				generationCountLabel,
				bestFitnessLabel,
				medianFitnessLabel
		);

		getChildren().addAll(simulationCanvas, vBox);

		appContext.getEventBus().listenFor(SimulatorDoneEvent.class, event -> Platform.runLater(() -> {
			Platform.runLater(() -> creatureCountLabel.setText(creatureCountString + creatureCount));
			creatureCount++;
		}));
		appContext.getEventBus().listenFor(SimulatorStartedEvent.class, event -> Platform.runLater(() -> {
			speciesLabel.setText(speciesString + event.getSpecies());
			mutationRateLabel.setText(mutationRateString + event.getMutationRate());
			structuralMutationRateLabel.setText(structuralMutationRateString + event.getStructuralMutationRate());
		}));
		appContext.getEventBus().listenFor(GenerationDoneEvent.class, event -> {
			creatureCount = 0;
			if (simulationModeButton.isDisabled()) {
				simulationModeButton.setDisable(false);
				appContext.getEventBus().emit(new SimulationModeChangedEvent(simulateGraphically));
			}
			Platform.runLater(() -> {
				creatureCountLabel.setText(creatureCountString + creatureCount);
				generationCountLabel.setText(generationCountString + event.getGenerationCount());
				bestFitnessLabel.setText(bestFitnessString + event.getBestDistance());
				medianFitnessLabel.setText(medianFitnessString + event.getMedianDistance());
			});
		});
	}
}
