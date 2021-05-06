package com.weisbrja.population;

import com.weisbrja.AppContext;
import com.weisbrja.data.CSVHelperFitness;
import com.weisbrja.data.CSVHelperMutationRate;
import com.weisbrja.data.CSVHelperSpecies;
import com.weisbrja.simulation.*;

import javax.vecmath.Vector2d;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Population {

	private final AppContext appContext;

	private final boolean saveData;
	private final int simulationCycleCount;
	private final ExecutorService simulatorExecutorService;
	private CSVHelperFitness csvHelperFitness;
	private CSVHelperMutationRate csvHelperMutationRate;
	private CSVHelperSpecies csvHelperSpecies;
	private Simulator[] simulators;
	private double[] probabilities;
	private int simulatorsDoneCount;
	private boolean simulateGraphically;
	private boolean simulationModeChanged;
	private int generationCount;
	private double bestFitness;
	private double medianFitness;
	private double medianMutationRate;
	private double medianStructuralMutationRate;

	public Population(AppContext appContext, boolean saveData, String filenameFitness, String filenameMutationRate, String filenameSpecies, int threadCount, int simulationCycleCount) {
		this.appContext = appContext;
		this.saveData = saveData;
		this.simulationCycleCount = simulationCycleCount;

		if (saveData) {
			csvHelperFitness = new CSVHelperFitness(filenameFitness);
			csvHelperMutationRate = new CSVHelperMutationRate(filenameMutationRate);
			csvHelperSpecies = new CSVHelperSpecies(filenameSpecies);
		}

		simulatorExecutorService = Executors.newFixedThreadPool(threadCount);

		appContext.getEventBus().listenFor(SimulationModeChangedEvent.class, this::handleSimulationModeChanged);
		appContext.getEventBus().listenFor(SimulatorDoneEvent.class, event -> handleSimulatorDone());
		appContext.getEventBus().listenFor(AllSimulatorsDoneEvent.class, event -> handleAllSimulatorsDone());
	}

	private void handleSimulatorDone() {
		simulatorsDoneCount++;
		if (simulatorsDoneCount == simulators.length)
			appContext.getEventBus().emit(new AllSimulatorsDoneEvent());
		else if (simulateGraphically)
			simulators[simulatorsDoneCount].startGraphically(simulationCycleCount);
		else if (simulationModeChanged) {
			simulationModeChanged = false;
			// finish all simulators fast
			List<Future<?>> futures = new ArrayList<>();
			for (int i = simulatorsDoneCount; i < simulators.length; i++) {
				Simulator simulator = simulators[i];
				futures.add(simulatorExecutorService.submit(() -> simulator.start(simulationCycleCount)));
			}
			new Thread(() -> {
				for (Future<?> future : futures)
					try {
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				appContext.getEventBus().emit(new AllSimulatorsDoneEvent());
			}).start();
		}
	}

	private void handleSimulationModeChanged(SimulationModeChangedEvent simulationModeChangedEvent) {
		simulationModeChanged = true;
		simulateGraphically = simulationModeChangedEvent.getSimulateGraphically();
	}

	private void handleAllSimulatorsDone() {
		simulatorsDoneCount = 0;

		// save the number of creatures in each species to a file
		Map<String, Integer> speciesCountMap = new HashMap<>();
		for (Simulator simulator : simulators) {
			String species = simulator.getCreature().getSpecies();
			if (!speciesCountMap.containsKey(species))
				speciesCountMap.put(species, 1);
			else
				speciesCountMap.replace(species, speciesCountMap.get(species) + 1);
		}

		// do the evolutionary algorithm
		doSelection();

		// save data
		if (saveData) {
			csvHelperFitness.printValue(generationCount, bestFitness, medianFitness);
			csvHelperMutationRate.printValue(generationCount, medianMutationRate, medianStructuralMutationRate);
			for (String species : speciesCountMap.keySet())
				csvHelperSpecies.printValues(generationCount, species, speciesCountMap.get(species));
		}

		doReproduction();

		generationCount++;
		appContext.getEventBus().emit(new GenerationDoneEvent(generationCount, bestFitness, medianFitness));

		startSimulating();
	}

	public void initialize(int populationSize) {
		// initialize the simulators with the given population size
		simulators = new Simulator[populationSize];
		for (int i = 0; i < populationSize; i++) {
			simulators[i] = new Simulator(appContext);
			simulators[i].generateRandomCreature();
		}
	}

	public void startSimulating() {
		if (simulateGraphically)
			simulators[0].startGraphically(simulationCycleCount);
		else {
			List<Future<?>> futures = new ArrayList<>();
			for (Simulator simulator : simulators)
				futures.add(simulatorExecutorService.submit(() -> simulator.start(simulationCycleCount)));
			new Thread(() -> {
				for (Future<?> future : futures)
					try {
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				appContext.getEventBus().emit(new AllSimulatorsDoneEvent());
			}).start();
		}
	}

	private void doSelection() {
		// calculate the fitness of each creature and the sum of all fitness values
		double sum = 0d;
		for (Simulator simulator : simulators) {
			simulator.calculateFitness();
			sum += simulator.getFitness();
		}

		// sort the simulators going from the simulator with the best to the simulator with the worst creature
		Arrays.sort(simulators, (simulator, t1) -> Double.compare(t1.getFitness(), simulator.getFitness()));

		double worstFitnessInGeneration = simulators[simulators.length - 1].getFitness();
		double offsetSum = sum - worstFitnessInGeneration * simulators.length;

		// calculate each absolute probability of being chosen as a parent creature
		if (offsetSum > 0d)
			for (Simulator simulator : simulators)
				simulator.setProbability((simulator.getFitness() - worstFitnessInGeneration) / offsetSum);
		else
			for (Simulator simulator : simulators)
				simulator.setProbability(1d / simulators.length);
		calculateProbabilities();

		// override the best fitness if a new one was achieved
		double bestFitnessInGeneration = simulators[0].getFitness();
		if (bestFitnessInGeneration > bestFitness)
			bestFitness = bestFitnessInGeneration;

		Simulator medianSimulator = simulators[simulators.length / 2];
		medianFitness = medianSimulator.getFitness();
		medianMutationRate = medianSimulator.getCreature().getMutationRate();
		medianStructuralMutationRate = medianSimulator.getCreature().getStructuralMutationRate();
	}

	private void doReproduction() {
		// keep the best creature
		simulators[0].getCreature().reset();

		// generate new creatures based on the last generation of creatures
		Creature[] newCreatures = new Creature[simulators.length - 1];

		for (int i = 0; i < simulators.length - 1; i++) {
			// get a parent creature based on its probability of being chosen
			Creature parentCreature = getRandomParentCreature();

			double mutationRate = getMutatedValue(parentCreature.getMutationRate(), appContext.getCreatureMutationRateBoundaries(), 0.01d);
			double structuralMutationRate = getMutatedValue(parentCreature.getStructuralMutationRate(), appContext.getCreatureStructuralMutationRateBoundaries(), 0.01d);
			Creature creature = new Creature(appContext, mutationRate, structuralMutationRate);

			// copy circles from parent creature
			for (Circle parentCircle : parentCreature.getCircles()) {
				double frictionPercentage = getMutatedValue(parentCircle.getFrictionPercentage(), 0d, 1d, mutationRate);

				double radius = getMutatedValue(parentCircle.getRadius(), appContext.getCircleRadiusBoundaries(), mutationRate);

				double startPositionX = getMutatedValue(parentCircle.getStartPosition().getX(), appContext.getCircleStartPositionBoundaries().getX(), mutationRate);
				double startPositionY = getMutatedValue(parentCircle.getStartPosition().getY(), appContext.getCircleStartPositionBoundaries().getYMin() - radius, appContext.getCircleStartPositionBoundaries().getYMax() - radius, mutationRate);
				Vector2d startPosition = new Vector2d(startPositionX, startPositionY);

				Circle circle = new Circle(appContext, frictionPercentage, radius, startPosition);
				creature.getCircles().add(circle);
			}

			// copy muscles from parent creature
			for (Muscle parentMuscle : parentCreature.getMuscles()) {
				Circle circle1 = creature.getCircles().get(parentCreature.getCircles().indexOf(parentMuscle.getCircle1()));
				Circle circle2 = creature.getCircles().get(parentCreature.getCircles().indexOf(parentMuscle.getCircle2()));

				double strength = getMutatedValue(parentMuscle.getStrength(), appContext.getMuscleStrengthBoundaries(), mutationRate);

				double lengthPhasesX = getMutatedValue(parentMuscle.getLengthPhases().getX(), appContext.getMuscleLengthPhasesBoundaries(), mutationRate);
				double lengthPhasesY = getMutatedValue(parentMuscle.getLengthPhases().getY(), appContext.getMuscleLengthPhasesBoundaries(), mutationRate);
				Vector2d lengthPhases = new Vector2d(lengthPhasesX, lengthPhasesY);

				double clockPhasesX = getMutatedValue(parentMuscle.getClockPhases().getX(), 0d, 1d, mutationRate);
				double clockPhasesY = getMutatedValue(parentMuscle.getClockPhases().getY(), 0d, 1d, mutationRate);
				Vector2d clockPhases = new Vector2d(clockPhasesX, clockPhasesY);

				double clockSpeed = getMutatedValue(parentMuscle.getClockSpeed(), appContext.getMuscleClockSpeedBoundaries(), mutationRate);

				Muscle muscle = new Muscle(appContext, circle1, circle2, strength, lengthPhases, clockPhases, clockSpeed);
				creature.getMuscles().add(muscle);
			}

			creature.adjustToGround();
			creature.calculatePossibleConnections();

			// apply structural mutations in some cases
			if (appContext.getRandomNumberGenerator().nextDouble() < structuralMutationRate)
				creature.addRandomCircle();
			if (appContext.getRandomNumberGenerator().nextDouble() < structuralMutationRate) {
				creature.removeRandomCircle();
				creature.adjustToGround();
			}
			if (appContext.getRandomNumberGenerator().nextDouble() < structuralMutationRate)
				creature.addRandomMuscle();
			if (appContext.getRandomNumberGenerator().nextDouble() < structuralMutationRate)
				creature.removeRandomMuscle();

			// save creature for later
			newCreatures[i] = creature;
		}

		// overwrite the creatures from the last generation with the new creators
		for (int i = 1; i < simulators.length; i++)
			simulators[i].setCreature(newCreatures[i - 1]);
	}

	private void calculateProbabilities() {
		probabilities = new double[simulators.length - 1];
		probabilities[0] = simulators[0].getProbability();
		for (int i = 1; i < probabilities.length; i++)
			probabilities[i] = probabilities[i - 1] + simulators[i].getProbability();
	}

	private Creature getRandomParentCreature() {
		double randomNumber = appContext.getRandomNumberGenerator().nextDouble();

		// find the index of the smallest probability larger than the random number
		// TODO: 12/11/20 implement a binary tree for every generation instead of a binary search for every creature
		int lowestIndex = 0;
		int highestIndex = probabilities.length - 1;
		int index = highestIndex / 2;

		while (lowestIndex <= highestIndex) {
			if (probabilities[index] < randomNumber)
				lowestIndex = index + 1;
			else
				highestIndex = index - 1;

			// get the index of the element in the middle of the lowest index and the highest index
			index = lowestIndex + (highestIndex - lowestIndex) / 2;
		}

		// return the chosen parent creature
		return simulators[index].getCreature();
	}

	private double getMutatedValue(double currentValue, double boundaryMin, double boundaryMax, double mutationRate) {
		double randomRangeMin = (boundaryMin - boundaryMax) / 2;
		double randomRangeMax = (boundaryMax - boundaryMin) / 2;
		return Math.min(Math.max(currentValue + appContext.getRandomNumberGenerator().getRandomRange(randomRangeMin, randomRangeMax) * mutationRate, boundaryMin), boundaryMax);
	}

	private double getMutatedValue(double currentValue, Boundary2d boundaries, double mutationRate) {
		return getMutatedValue(currentValue, boundaries.getMin(), boundaries.getMax(), mutationRate);
	}
}
