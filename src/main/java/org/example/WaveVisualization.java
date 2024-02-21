/*
 * 
 */
package org.example;

import org.example.WaveFormService.WaveFormJob;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * The Class Visualizer.
 *
 * @author GOXR3PLUS
 */
public class WaveVisualization extends WaveFormPane {
	
	/*** This Service is constantly repainting the wave */
	private final PaintService animationService;
	
	/*** This Service is creating the wave data for the painter */
	private final WaveFormService waveService;
	
	private boolean recalculateWaveData;
	public double TimerIncrement=1;
	public double WavLengthInSec2=1;
	public double someShitForPlayOnSpace=1;
	private double t1=0.0;
	private double t2=0.0;
	private Controller contr;
	private WavPlayer rec;
	/**
	 * Constructor
	 * 
	 * @param width
	 * @param height
	 */
	public WaveVisualization(int width, int height,double WavLen1,WavPlayer rec, Controller contr) {
		super(width, height);
		super.setWaveVisualization(this);
		this.rec=rec;
		WavLengthInSec2=WavLen1;
		double temp=WavLengthInSec2;
		TimerIncrement=width/temp/8.92;
		waveService = new WaveFormService(this);
		animationService = new PaintService();
		
		// ----------
		widthProperty().addListener((observable , oldValue , newValue) -> {
			//System.out.println("New Visualizer Width is:" + newValue);
			//TimerIncrement=0;
		//	rec.pause();

			// Canvas Width
			this.width = Math.round(newValue.floatValue());


			//Draw single line :)
			recalculateWaveData = true;
			clear();
            syncMusicAndDraw();
			//rec.play();
			TimerIncrement=this.width/temp/8.92;
		});
		// -------------
		heightProperty().addListener((observable , oldValue , newValue) -> {
			//System.out.println("New Visualizer Height is:" + newValue);
			
			// Canvas Height
			this.height = Math.round(newValue.floatValue());
			
			//Draw single line :)
			recalculateWaveData = true;
			clear();
		});
		
		//Tricky mouse events
		setOnMousePressed(m->{
			this.setTimerXPosition((int)m.getX());
			this.setTimerStartPosition((int)m.getX());
            Controller.rec.jumpTo(m.getX()/this.width);
			someShitForPlayOnSpace=m.getX()/this.width;
			if (m.isControlDown()) {
				t1=WavLengthInSec2*(int)m.getX()/this.width; //для передачи в CtrlRecService
			}
		});

		setOnMouseDragged(m->{
			this.setMouseXPosition((int) m.getX());
			this.setTimerEndPosition((int)m.getX());
		});
		setOnMouseMoved(m->{
           this.setMouseXPosition((int) m.getX());
		});
		setOnMouseReleased(m->{
			this.setTimerEndPosition((int)m.getX());
			if (TimerEndPosition<TimerStartPosition){
				this.setTimerXPosition(TimerEndPosition);
			}
			if (m.isControlDown()) {
				t2=WavLengthInSec2*(int)m.getX()/this.width; //для передачи в CtrlRecService
				CtrlRecService RecServ =new CtrlRecService(t1,t2,contr,waveService.getFileAbsolutePath());
				RecServ.start();
			}
		});

	}
	//--------------------------------------------------------------------------------------//
	
	/**
	 * @return the animationService
	 */
	public PaintService getAnimationService() {
		return animationService;
	}
	
	public WaveFormService getWaveService() {
		return waveService;
	}
	
	//--------------------------------------------------------------------------------------//
	
	/**
	 * Stars the wave visualiser painter
	 */
	public void startPainterService() {
		animationService.start();
	}
	
	/**
	 * Stops the wave visualiser painter
	 */
	public void stopPainterService() {
		animationService.stop();
		clear();
	}
	public void stopPainterServiceWithoutClear() {
		animationService.stop();

	}
	/**
	 * @return True if AnimationTimer of Visualiser is Running
	 */
	public boolean isPainterServiceRunning() {
		return animationService.isRunning();
	}
	
	/*-----------------------------------------------------------------------
	 * 
	 * -----------------------------------------------------------------------
	 * 
	 * 
	 * 							      Paint Service
	 * 
	 * -----------------------------------------------------------------------
	 * 
	 * -----------------------------------------------------------------------
	 */
	/**
	 * This Service is updating the visualizer.
	 *
	 * @author GOXR3PLUS
	 */
	public class PaintService extends AnimationTimer {
		
		/*** When this property is <b>true</b> the AnimationTimer is running */
		private volatile SimpleBooleanProperty running = new SimpleBooleanProperty(false);
		
		private long previousNanos = 0;
		
		@Override
		public void start() {
			// Values must be >0
			if (width <= 0 || height <= 0)
				width = height = 1;
			
			super.start();
			running.set(true);
		}
		
		public WaveVisualization getWaveVisualization() {
			return WaveVisualization.this;
		}
		
		@Override
		public void handle(long nanos) {
			//System.out.println("Running...")
			
			//Every 300 millis update
			if (nanos >= previousNanos + 100000 * 1000) { //
				previousNanos = nanos;
				setTimerXPosition(getTimerXPosition() + TimerIncrement);
			}
			
			//If resulting wave is not calculated
			if (getWaveService().getResultingWaveform() == null || recalculateWaveData) {
				
				//Start the Service
				getWaveService().startService(getWaveService().getFileAbsolutePath(), WaveFormJob.AMPLITUDES_AND_WAVEFORM);
				recalculateWaveData = false;
				
				return;
			}
			
			//Paint			
			paintWaveForm();
		}
		
		@Override
		public void stop() {
			super.stop();
			running.set(false);
		}
		
		/**
		 * @return True if AnimationTimer is running
		 */
		public boolean isRunning() {
			return running.get();
		}
		
		/**
		 * @return Running Property
		 */
		public SimpleBooleanProperty runningProperty() {
			return running;
		}
		
	}
	public void syncMusicAndDraw(){
		setTimerXPosition(rec.getCurrentTimeInSeconds()*this.width/WavLengthInSec2);
	}
}
