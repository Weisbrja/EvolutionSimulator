package com.weisbrja.simulation;

import com.weisbrja.AppContext;
import com.weisbrja.view.SimulationDrawBackgroundEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Simulator {

	private Timeline timeline;
	private Creature creature;

	private double fitness;
	private double probability;

	public Simulator() {
	}

	public Simulator(Creature creature) {
		this.creature = creature;
	}

	public void generateRandomCreature() {
		creature = new Creature();
		creature.randomize();

		// generate random circles
		int randomCircleCount = 2 + AppContext.getInstance().getRandomNumberGenerator().nextInt(4);
		for (int i = 0; i < randomCircleCount; i++) {
			Circle circle = new Circle();
			circle.randomize();
			creature.getCircles().add(circle);
		}
		creature.adjustToGround();
		creature.calculatePossibleConnections();

		// generate random muscles
		int maxMuscleCount = randomCircleCount * (randomCircleCount - 1) / 2;
		int randomMuscleCount = Math.min(2, AppContext.getInstance().getRandomNumberGenerator().nextInt(maxMuscleCount + 1));
		for (int i = 0; i < randomMuscleCount; i++)
			creature.addRandomMuscle();
	}

	private void nextSimulationStep() {
		creature.applyForceY(AppContext.getInstance().getGravityY());
		creature.update();
	}

	private void nextDrawStep() {
		AppContext.getInstance().getEventBus().emit(new SimulationDrawBackgroundEvent());
		AppContext.getInstance().getEventBus().emit(new CreatureDrawEvent(creature));
	}

	public void start(int cycleCount) {
		// simulate the creature as fast as possible for the given number of cycles
		for (int i = 0; i < cycleCount; i++) {
			if (AppContext.getInstance().getStopCreaturesWhenOnGround() && creature.getOnGround())
				break;
			else
				nextSimulationStep();
		}
	}

	public void startGraphically(int cycleCount) {
		// start the timeline for the creature updates and drawing
		timeline = new Timeline(new KeyFrame(Duration.millis(10L), actionEvent -> {
			if (AppContext.getInstance().getStopCreaturesWhenOnGround() && creature.getOnGround()) {
				timeline.stop();
				AppContext.getInstance().getEventBus().emit(new SimulatorDoneEvent());
			} else {
				nextSimulationStep();
				nextDrawStep();
			}
		}));
		timeline.setCycleCount(cycleCount);
		timeline.setOnFinished(actionEvent -> AppContext.getInstance().getEventBus().emit(new SimulatorDoneEvent()));
		timeline.play();
		AppContext.getInstance().getEventBus().emit(new SimulatorStartedEvent(creature.getSpecies(), creature.getMutationRate(), creature.getStructuralMutationRate()));
	}

	public Creature getCreature() {
		return creature;
	}

	public void setCreature(Creature creature) {
		this.creature = creature;
	}

	public void calculateFitness() {
		fitness = creature.getFitness();
	}

	public double getFitness() {
		return fitness;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}
}
