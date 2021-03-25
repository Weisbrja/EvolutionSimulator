package de.weisbrja.view;

import de.weisbrja.AppContext;
import de.weisbrja.simulation.Circle;
import de.weisbrja.simulation.CreatureDrawEvent;
import de.weisbrja.simulation.Muscle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javax.vecmath.Vector2d;

public class SimulationCanvas extends Pane {

	private final AppContext appContext;

	private final Canvas canvas;

	private final Vector2d cameraPosition;
	private final Vector2d cameraPositionOffset;

	public SimulationCanvas(AppContext appContext) {
		this.appContext = appContext;

		canvas = new Canvas(1920d, 1080d);
		getChildren().add(canvas);

		cameraPositionOffset = new Vector2d(300d, 600d);
		cameraPosition = (Vector2d) cameraPositionOffset.clone();
		cameraPosition.scale(-1d);

		appContext.getEventBus().listenFor(SimulationDrawBackgroundEvent.class, event -> handleDrawBackground());
		appContext.getEventBus().listenFor(CreatureDrawEvent.class, this::handleCreatureDraw);
	}

	private void handleDrawBackground() {
		GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

		// draw the blue sky
		graphicsContext.setFill(Color.rgb(60, 180, 255));
		graphicsContext.fillRect(0d, 0d, canvas.getWidth(), canvas.getHeight());

		double width = 100d;
		for (double i = -cameraPosition.getX() % (2d * width) - width; i <= canvas.getWidth(); i += width * 2d) {
			graphicsContext.setFill(Color.rgb(50, 150, 200));
			graphicsContext.fillRect(i, 0d, width, canvas.getHeight());
		}

		// draw the starting line
		graphicsContext.setStroke(Color.rgb(200, 0, 0));
		graphicsContext.setLineWidth(2d);
		graphicsContext.beginPath();
		graphicsContext.moveTo(-cameraPosition.getX(), 0d);
		graphicsContext.lineTo(-cameraPosition.getX(), canvas.getHeight());
		graphicsContext.stroke();

		// draw the green ground
		graphicsContext.setFill(Color.rgb(30, 130, 0));
		graphicsContext.fillRect(0d, -cameraPosition.getY(), canvas.getWidth(), canvas.getHeight());
	}

	private void handleCreatureDraw(CreatureDrawEvent creatureDrawEvent) {
		GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

		// draw the muscles
		for (int i = creatureDrawEvent.getCreature().getMuscles().size() - 1; i >= 0; i--) {
			Muscle muscle = creatureDrawEvent.getCreature().getMuscles().get(i);
			double alphaPercentageStart = 50d / 255d;
			double alphaPercentageEnd = 255d / 255d;
			double alphaPercentage = alphaPercentageStart + (alphaPercentageEnd - alphaPercentageStart) / (appContext.getMuscleStrengthBoundaries().getMax() - appContext.getMuscleStrengthBoundaries().getMin()) * (muscle.getStrength() - appContext.getMuscleStrengthBoundaries().getMin());
			if (muscle.getExpanding())
				graphicsContext.setStroke(Color.rgb(200, 50, 50, alphaPercentage));
			else
				graphicsContext.setStroke(Color.rgb(50, 50, 200, alphaPercentage));
			graphicsContext.setLineWidth(muscle.getExpanding() ? 8d : 12d);
			graphicsContext.beginPath();
			graphicsContext.moveTo(muscle.getCircle1().getPosition().getX() - cameraPosition.getX(), muscle.getCircle1().getPosition().getY() - cameraPosition.getY());
			graphicsContext.lineTo(muscle.getCircle2().getPosition().getX() - cameraPosition.getX(), muscle.getCircle2().getPosition().getY() - cameraPosition.getY());
			graphicsContext.stroke();
		}

		// draw the circles
		for (int i = creatureDrawEvent.getCreature().getCircles().size() - 1; i >= 0; i--) {
			Circle circle = creatureDrawEvent.getCreature().getCircles().get(i);
			double grayScalePercentageStart = 255d / 255d;
			double grayScalePercentageEnd = 0d / 255d;
			double grayScalePercentage = grayScalePercentageStart + (grayScalePercentageEnd - grayScalePercentageStart) * circle.getFrictionPercentage();
			graphicsContext.setFill(Color.color(grayScalePercentage, grayScalePercentage, grayScalePercentage));
			graphicsContext.fillOval(circle.getPosition().getX() - circle.getRadius() - cameraPosition.getX(), circle.getPosition().getY() - circle.getRadius() - cameraPosition.getY(), circle.getDiameter(), circle.getDiameter());
		}

		// update the camera position
		Vector2d cameraTargetPosition = (Vector2d) creatureDrawEvent.getCreature().getPosition().clone();
		cameraTargetPosition.sub(cameraPositionOffset);
		cameraPosition.interpolate(cameraTargetPosition, 0.02d);
	}
}