package org.example;

import javafx.scene.paint.Color;

/**
 * Swing panel paints the waveform of a track.
 *
 * @author GOXR3PLUS STUDIO
 */
public class WaveFormPane extends ResizableCanvas {
	
	private final float[] defaultWave;
	private float[] waveData;
	private Color backgroundColor;
	private Color foregroundColor;
	private Color transparentForeground;
	private Color mouseXColor = Color.rgb(255, 255, 255, 0.7);
	int width;
	int height;
	private double timerXPosition = 0;
	private int mouseXPosition = -1;
	private WaveVisualization waveVisualization;
	public int TimerStartPosition=0;
	public int TimerEndPosition=0;
	/**
	 * Constructor
	 * 
	 * @param width
	 * @param height
	 */
	public WaveFormPane(int width, int height) {
		defaultWave = new float[width];
		this.width = width;
		this.height = height;
		this.setWidth(width);
		this.setHeight(height);
		//this.maxWidth(width);
		//this.maxHeight(height);
		
		//Create the default Wave
		for (int i = 0; i < width; i++)
			defaultWave[i] = 0.28802148f;
		waveData = defaultWave;
		
		backgroundColor = Color.web("#252525");
		setForeground(Color.ORANGE);
		
	}
	
	/**
	 * Set the WaveData
	 * 
	 * @param waveData
	 */
	public void setWaveData(float[] waveData) {
		this.waveData = waveData;
	}
	
	public void setForeground(Color color) {
		this.foregroundColor = color;
		transparentForeground = Color.rgb((int) ( foregroundColor.getRed() * 255 ), (int) ( foregroundColor.getGreen() * 255 ), (int) ( foregroundColor.getBlue() * 255 ), 0.3);
	}
	
	public void setBackgroundColor(Color color) {
		this.backgroundColor = color;
	}
	
	public double getTimerXPosition() {
		return timerXPosition;
	}
	
	public void setTimerXPosition(double timerXPosition) {
		this.timerXPosition = timerXPosition;
	}
	public void setTimerStartPosition(int timerXPosition) {
		this.TimerStartPosition = timerXPosition;
	}
	public void setTimerEndPosition(int timerXPosition) {
		this.TimerEndPosition = timerXPosition;
	}
	public void setMouseXPosition(int mouseXPosition) {
		this.mouseXPosition = mouseXPosition;
	}

	/**
	 * Clear the waveform
	 */
	public void clear() {
		waveData = defaultWave;
		
		//Draw a Background Rectangle
		gc.setFill(backgroundColor);
		gc.fillRect(0, 0, width, height);
		
		//Paint a line
		gc.setStroke(foregroundColor);
		gc.strokeLine(0, height / 2, width, height / 2);
	}
	
	/**
	 * Paint the WaveForm
	 */
	public void paintWaveForm() {
		
		//Draw a Background Rectangle
		gc.setFill(backgroundColor);
		gc.fillRect(0, 0, width, height);
		
		//Draw the waveform
		gc.setStroke(foregroundColor);
		if (waveData != null)
			for (int i = 0; i < waveData.length; i++) {
				if (!waveVisualization.getAnimationService().isRunning()) {
					clear();
					break;
				}
				int value = (int) ( waveData[i] * height );
				int y1 = ( height - 2 * value ) / 2;
				int y2 = y1 + 2 * value;
				gc.strokeLine(i, y1, i, y2);
			}
		
		//Draw a semi transparent Rectangle
		gc.setFill(transparentForeground);
		if(TimerEndPosition>TimerStartPosition){
			gc.fillRect(TimerStartPosition, 0, TimerEndPosition-TimerStartPosition, height);

		} else {
			gc.fillRect(TimerEndPosition, 0, Math.abs(TimerEndPosition-TimerStartPosition), height);

		}

		
		//Draw an horizontal line
		gc.setFill(Color.WHITE);
		gc.fillOval(timerXPosition, 0, 1, height);
		
		//Draw an horizontal line
		if (mouseXPosition != -1) {
			gc.setFill(mouseXColor);
			gc.fillRect(mouseXPosition, 0, 1, height);
		}
	}
	
	public WaveVisualization getWaveVisualization() {
		return waveVisualization;
	}
	
	public void setWaveVisualization(WaveVisualization waveVisualization) {
		this.waveVisualization = waveVisualization;
	}
	
}
