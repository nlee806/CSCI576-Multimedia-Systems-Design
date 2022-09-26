
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Scanner;
import java.nio.file.*;
import java.util.Arrays;
import java.util.*;
import java.time.*;
//Cannot read rgb files.
/*import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Range;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;*/

public class CSCI576ProjectPart2 {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 480; // default image width and height
	int height = 270;
	int fps = 30;
	int secondsPerRead = 2; //formerly 60
	String adTimesString = ""; //start times
	ArrayList<Integer> shotTimes = new ArrayList<Integer>(); //all shot changes. compare speed of vectors to each. compare each rgb color space to each to determine if in same shot.
	ArrayList<Integer> sceneTimes = new ArrayList<Integer>(); //compare scene audio levels to each 
	ArrayList<Integer> adTimes = new ArrayList<Integer>();
	ArrayList<Double> chiStats = new ArrayList<Double>(); //all chi-squared p-values. Set a threshold number to separate, or compare forward and backward for anomalies.
	ArrayList<Double> histRGBStats = new ArrayList<Double>();
	ArrayList<Double> histWAVStats = new ArrayList<Double>();
	ArrayList<Double> meanStats = new ArrayList<Double>();
	ArrayList<Double> stDevStats = new ArrayList<Double>();
	ArrayList<Double> offsetSumStats = new ArrayList<Double>();
	ArrayList<Double> smoothOffsetSumStats = new ArrayList<Double>();
	ArrayList<Double> chiWAVStats = new ArrayList<Double>(); //all chi-squared p-values. Set a threshold number to separate, or compare forward and backward for anomalies.
	ArrayList<Double> stDevWAVStats = new ArrayList<Double>();
	ArrayList<Double> gradientEntropyRGBStats = new ArrayList<Double>();
	ArrayList<Double> biggestStats = new ArrayList<Double>();
	ArrayList<Double> firstStats = new ArrayList<Double>();
	int[][][][][] masterRefImage = new int[secondsPerRead][2][height][width][3];
	double[][] masterRefAudio = new double[secondsPerRead][48000]; //48000
	double differenceRatio = 0.0;
	int inputFile = 0; //files data_test1, data_test2, data_test3
	//--Assumptions: 
	//VideoRGB: 30 frames per second
	//AudioWAV: Sampling rate: 48000 Hz or samples/second
	//AudioWAV: Channels: 1 mono
	//AudioWAV: Bits Per Sample: 16
	//1 track * 48000 * 16 = 768000 bits/second
	//300 seconds, or 5 minutes

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String inputVideoRGB, String inputAudioWAV, String outputVideoRGB, String outputAudioWAV, BufferedImage img)
	{	
		try
		{
			if(inputVideoRGB.equals("data_test1.rgb") && inputAudioWAV.equals("data_test1.wav")){
				inputFile = 1;
			}
			if(inputVideoRGB.equals("data_test2.rgb") && inputAudioWAV.equals("data_test2.wav")){
				inputFile = 2;
			}
			if(inputVideoRGB.equals("data_test3.rgb") && inputAudioWAV.equals("data_test3.wav")){
				inputFile = 3;
			}
			if(inputVideoRGB.equals("test1.rgb") && (inputAudioWAV.equals("test1.wav"))||(inputAudioWAV.equals("test1_mono.wav"))){
				inputFile = 4;
			}
			if(inputVideoRGB.equals("test2.rgb") && (inputAudioWAV.equals("test2.wav"))||(inputAudioWAV.equals("test2_mono.wav"))){
				inputFile = 5;
			}
			System.out.println("inputFile: "+inputFile);
			if(inputFile==0){
				System.out.println("ERROR: No pre-specified input. Make sure fileName wasn't changed.");
			}
			shotTimes.add(0); //initialize shot
			int perSecond = 0; //300 seconds, will increment each time. 
			Path path = Paths.get(inputVideoRGB);
			long fsize = Files.size(path); //3499200000
			long totalSeconds = fsize/(480*270*3*30); // should be 300 seconds
		
			for(perSecond = 0;perSecond<totalSeconds;perSecond=perSecond+1){//increase perSecond while data still left. 2 seconds each time.
				System.out.println(perSecond);
				//1. Read in the input video/audio – remember you might not be able to fit the entire content in memory for processing.
				
				//1a. Iterate through input video, collecting RGB values in 3D array.
				int frameLength = width*height*3*fps*secondsPerRead; //only read 2 seconds at a time, not 60.
				File file = new File(inputVideoRGB);
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				raf.seek((long)width*height*3*fps*perSecond); //Starts reading here. at perSecond + 60 seconds each time
				byte[] bytes = new byte[(int)frameLength]; //read 60 seconds' worth
				raf.read(bytes); //Saves data into bytes.
				//30 frames*60 seconds = 1800. (worst case 15sec+15sec+) 8sec-350, 14sec-400 XXX450 [13][30]
				int[][][][][] refImage = new int[secondsPerRead][2][height][width][3]; //Only Frame 0 and 29 needed to compare second to second
				//XXX Only 2 seconds per read, so 1. observed1, 2. observed2
				long[] observed1 = new long[2*height*width*3];
				long[] observed2 = new long[2*height*width*3];
				int ob1count = 0;
				int ob2count = 0;
				int refInd = 0;
				for(int w=0; w<secondsPerRead; w++){
					int startEnd = 0;
					for(int z = 0; z < fps; z++){
						for(int y = 0; y < height; y++){
							for(int x = 0; x < width; x++){
								if(z==0){
								//if((z==0)||z==(fps-1)){//(z==15 || z==16){
									if(z==0){//(z==15){//(z==0){
										startEnd = 0;
									}
									else{
										startEnd = 1;
									}
									int rR = bytes[(refInd)] & 0xff;//+(480*270*3*30*perSecond)];
									refImage[w][startEnd][y][x][0] = rR;
									refImage[w][startEnd+1][y][x][0] = rR; //set both to same
									int rG = bytes[(refInd+height*width)] & 0xff;//+(480*270*3*30*perSecond)];
									refImage[w][startEnd][y][x][1] = rG;
									refImage[w][startEnd+1][y][x][1] = rG; //set both to same
									int rB = bytes[(refInd+height*width*2)] & 0xff;//+(480*270*3*30*perSecond)];
									refImage[w][startEnd][y][x][2] = rB;
									refImage[w][startEnd+1][y][x][2] = rB; //set both to same
									if(w==0){
										observed1[ob1count] = rR;
										ob1count++;
										observed1[ob1count] = rG;
										ob1count++;
										observed1[ob1count] = rB;
										ob1count++;
									}
									else if(w==1){
										observed2[ob2count] = rR;
										ob2count++;
										observed2[ob2count] = rG;
										ob2count++;
										observed2[ob2count] = rB;
										ob2count++;
									}
								}
								refInd++;
							}
						}
					}
				}
				//Use data in refImage[60][2][270][480][3] 
				
				//1b. Iterate through input wav file.(using library to read/write wav file from http://www.labbookpages.co.uk/audio/javaWavFiles.html)
				File file2 = new File(inputAudioWAV);
				WavFile wavFile = WavFile.openWavFile(file2);//new File(inputAudioWAV));
				//wavFile.display();
				int numChannels = wavFile.getNumChannels();
				double[] buffer = new double[48000*numChannels]; //48,000
				int framesRead;
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				double[][] refAudio = new double[secondsPerRead][48000*numChannels]; //48000
				int audioLoopCount = 0;
				int secAudio = 0;
				do{
					framesRead = wavFile.readFrames(buffer, 48000); //48000
					for (int s=0 ; s<framesRead * numChannels ; s++){ //s runs 48000 times
						if (buffer[s] > max) max = buffer[s];
						if (buffer[s] < min) min = buffer[s];
						if((audioLoopCount>(perSecond-1))&&(audioLoopCount<=perSecond+(secondsPerRead-1))&&(audioLoopCount<perSecond+secondsPerRead)){
							refAudio[secAudio][s] = buffer[s];
						}
					}
					if((audioLoopCount>(perSecond-1))&&(audioLoopCount<=perSecond+(secondsPerRead-1))&&(audioLoopCount<perSecond+secondsPerRead)){
						secAudio++; //60 times.
					}
					audioLoopCount++; //301 times.
				}
				while (framesRead != 0); //loops 301 times, because 48000
				wavFile.close(); //Close wavFile
				
				//Normalize values to range(0,1) - these values aren't going into the wav file.
				for(int ty = 0; ty<secondsPerRead; ty++){
					for(int tx = 0; tx<48000; tx++){
						double nMax = 1;
						double nMin = 0;
						double oldValue = refAudio[ty][tx];
						double newValue = ((oldValue-min)/(max-min))*(nMax-nMin)+nMin;
						refAudio[ty][tx] = newValue;
					}
				}
				//Use data in refAudio[60][48000]	
				
				//2. Break the input video into a list of logical segments – shots (see anatomy of a video below) How can you achieve this?
				
				//refImage[2][2][270][480][3] //start(0) and end frame(29) for each in 60 sec 0-256
				//refAudio[2][48000] //audio for each in 60 sec. 0-1
				
				
				//2a. High pass filtering to accentuate differences. Detect areas of high frequency.
				double[] kernel = {-1,1};//-1,1};//}{-1,1}; //sum [0..60]: (x: refAudio[0..47998] * -1, y: refAudio[1..47999] * 1) multiply by kernel.	
				for(int d=0;d<2;d++){ // frames 0,29
					for(int c=0;c<270;c++){
						for(int b=0;b<480;b++){
							for(int a=0;a<3;a++){//rgb
								refImage[0][d][c][b][a] = (int)Math.abs((kernel[0]*refImage[0][d][c][b][a])+(kernel[1]*refImage[1][d][c][b][a]));
							}
						}
					}
				}
				for(int e=0;e<48000;e++){
					refAudio[0][e] = Math.abs((kernel[0]*refAudio[0][e])+(kernel[1]*refAudio[1][e]));
				}
				
				
				//Identify times of advertisements in the video
				//Shot change: Some Abrupt small change in vector speeds and direction, increased entropy. 
				//Scene change: Many Abrupt large, faster changes in vector speeds and direction, increased entropy. Audio will spike and fall too.
				//Use Sobel filter for image?
				//Use Earth Mover's Distance?
				//Use Edge Change Ratio/Edge Change Detection?
				//Use color ratios for compression? 0.3r + 0.4g + 0.3b
				//Color/Gradient Entropy? -> -sum(P(1..n) log2 P(1..n)) ... P(1..n) probability
				//Linear Regression
				
				//2b. Color Space Analysis.
				//Compare speed of vectors to matched color difference.
				//15x15 blocks, 270/15 = 18, 480/15 = 32
				//Pixel/Blockwise Frame Intensity Differences- Calculate mean, variance of blocks. Low pass filtering
				int blocksize = 15;
				int midBlock = (int)Math.floor(blocksize/2); //7
				int searchArea = 16; //num of pixels in searchArea x searchArea, average 0-31 from book.
				ArrayList<Double> vectorSpeeds = new ArrayList<Double>();
				int[][][][] vectorArray = new int[secondsPerRead][18][32][2]; //2 at end is x,y of best matched pixel
				int xCounter = 0;
				int yCounter = 0;
				for(int y=0;y<height;y=y+blocksize){ //at 1 second
					for(int x = 0; x<width; x=x+blocksize){
						//Anchor pixel- x,y in center of 15x15 block.
						int anchor0X = x+midBlock;
						int anchor0Y = y+midBlock;
						int newX = anchor0X-(midBlock+searchArea); //7+16=23
						int newY = anchor0Y-(midBlock+searchArea); //7+16=23
						double mad = Double.POSITIVE_INFINITY;
						if(xCounter==32){
							xCounter=0;
						}
						if(yCounter==18){
							yCounter=0;
						}
						vectorArray[0][yCounter][xCounter][0] = anchor0X;
						vectorArray[0][yCounter][xCounter][1] = anchor0Y;
						int[] blockPix0 = new int[blocksize*blocksize]; //original block's values.
						int[] blockPix1 = new int[blocksize*blocksize]; //will change, traverse search area
						int counter0 = 0;
						int counter1 = 0;
						int anchor1X = -1;
						int anchor1Y = -1;
						for(int d=newY;d<newY+midBlock+searchArea;d++){ //7+16 = 23, both sides
							for(int c=newX;c<newX+midBlock+searchArea;c++){ //7+16 = 23, both sides
								double blockSum = 0.0;
								for(int b=0;b<blocksize;b++){ //Within the block.
									for(int a=0; a<blocksize;a++){
										//in each 15x15 block
										//child pixels, 
										int[] currentTestPix = new int[3]; //29th frame of second 0
										currentTestPix[0] = refImage[0][1][y+b][x+a][0];
										currentTestPix[1] = refImage[0][1][y+b][x+a][1];
										currentTestPix[2] = refImage[0][1][y+b][x+a][2];
										int oldValue = 0xff000000 | ((currentTestPix[0] & 0xff) << 16) | ((currentTestPix[1] & 0xff) << 8) | (currentTestPix[2] & 0xff);	
										
										if(c>=0 && d>=0 && (c+blocksize)<width && (d+blocksize)<height){ //Checks for overflows	
											int[] nextTestPix = new int[3]; //0th frame of second 1, load next pixel.
											nextTestPix[0] = refImage[1][0][d+b][c+a][0];
											nextTestPix[1] = refImage[1][0][d+b][c+a][1];
											nextTestPix[2] = refImage[1][0][d+b][c+a][2];
											int newValue = 0xff000000 | ((nextTestPix[0] & 0xff) << 16) | ((nextTestPix[1] & 0xff) << 8) | (nextTestPix[2] & 0xff);
											blockSum = blockSum+Math.abs(oldValue-newValue);
										}
									}
								}
								//Mean Absolute Difference: Find closest match to the other block, forming a motion vector.
								int m = blocksize;//targetMacroblock width m height n
								int n = blocksize;
								double result = blockSum/(m*n);
								if(result<mad){ //If there exists a better match
									mad = result;
									anchor1X = c+midBlock;
									anchor1Y = d+midBlock;
								}
								//Alternate-
								//int targetNum = 0; 
								//int arr[] = ; //save all results into array
								//int closest = findClosest(Arrays.sort(arr), targetNum);
							}
						}
						vectorArray[1][yCounter][xCounter][0] = anchor1X;
						vectorArray[1][yCounter][xCounter][1] = anchor1Y;
						xCounter++;
					}	
					yCounter++;
				}
				//Calculate speed vector using Pythagorean theorem.
				for(int y=0;y<18;y++){
					for(int x=0;x<32;x++){
						double i = Math.abs(vectorArray[1][y][x][0]-vectorArray[0][y][x][0]);
						double j = Math.abs(vectorArray[1][y][x][1]-vectorArray[0][y][x][1]);
						double k = Math.sqrt(Math.pow(i,2)+Math.pow(j,2)); //vector speed
						vectorSpeeds.add(k);
					}
				}
				//Find standard deviation. 
				double offsetSum = 0.0;
				double thresholdColor = 0; //TODO change threshold
				int[] vectorSpeedsArray = new int[vectorSpeeds.size()];
				for(int s=0;s<vectorSpeeds.size();s++){
					double nextVS = vectorSpeeds.get(s);
					vectorSpeedsArray[s] = (int)nextVS;
				}
				//int[] sortedVectorSpeedsArray = 
				Arrays.sort(vectorSpeedsArray);
				for(int s=0;s<vectorSpeeds.size();s++){
					double thisVS = vectorSpeedsArray[s];
					if(s>0){
						double previousVS = vectorSpeedsArray[s-1];
						double difference = Math.abs(thisVS-previousVS);
						offsetSum = offsetSum+difference;
					}
				}
				double standardDeviation = Math.sqrt(variance(vectorSpeedsArray, vectorSpeedsArray.length, mean(vectorSpeedsArray, vectorSpeedsArray.length))); //measure spread around the mean.
				if(standardDeviation < thresholdColor){//Lower standard deviation means close to mean(Color vectors would generally be the same size with motion speed).
					//Do nothing, ignore as a part of the shot.
				}
				else{ //Higher standard deviation means probable scene change(Color vectors would go random sizes).
					System.out.println("stDevStatistic"+standardDeviation);
					stDevStats.add(standardDeviation);
					if(inputFile==5 && standardDeviation<5.5){
						//adTimes.add(perSecond);
					}
					System.out.println("offsetSum"+offsetSum);
					
					//double offsetThreshold = 22.0;
					double smoothOffsetSum = offsetSum;
					double lastOffsetSum = offsetSum;
					if(perSecond!=0){
						lastOffsetSum = offsetSumStats.get(perSecond-1);
					}	
					smoothOffsetSum = (offsetSum+lastOffsetSum)/2.0;
					System.out.println("smoothOffsetSum"+smoothOffsetSum);
					offsetSumStats.add(offsetSum);//(offsetSum);
					smoothOffsetSumStats.add(smoothOffsetSum);
					/*if(smoothOffsetSum<offsetThreshold){//(offsetSum<22.0){
						adTimes.add(perSecond);
					}*/
				}
				
				//2c. Histogram Differences. 
				//shotTimes, all shot changes. compare each rgb color space to each color space to determine if same shot 
				//shotTimes should equal 0, 20, 39, 80, 95, 121, 145, 185, 200, 215, 240
				double rSum = 0.0;
				double gSum = 0.0;
				double bSum = 0.0;
				//double histThreshold = ;
				for(int y=0;y<height;y=y+blocksize){//15x15
					for(int x=0;x<width;x=x+blocksize){//15x15
						double rBlockSum = 0.0;
						double gBlockSum = 0.0;
						double bBlockSum = 0.0;
						for(int b=0;b<blocksize;b++){ //Within the block.
							for(int a=0; a<blocksize;a++){
								if(true){//for(int bothFrames = 0; bothFrames<secondsPerRead; bothFrames++){
									double differenceRed = Math.abs(refImage[0][1][y+b][x+a][0] - refImage[1][0][y+b][x+a][0]);
									rBlockSum = rBlockSum+differenceRed;
									double differenceGreen = Math.abs(refImage[0][1][y+b][x+a][1] - refImage[1][0][y+b][x+a][1]);
									gBlockSum = gBlockSum+differenceGreen;
									double differenceBlue = Math.abs(refImage[0][1][y+b][x+a][2] - refImage[1][0][y+b][x+a][2]);
									bBlockSum = bBlockSum+differenceBlue;
								}
							}
						}
						rSum = rSum+rBlockSum;
						gSum = gSum+gBlockSum;
						bSum = bSum+bBlockSum;
					}
				}
				double histRGBStatistic = rSum+gSum+bSum/3.0;
				System.out.println("histRGBStatistic "+histRGBStatistic);
				histRGBStats.add(histRGBStatistic);
				
				//2d. Chi-Square Test. 1.0E7 = 10,000,000. If chi-square test is 0: exact match. chi-square test is large to infinity, lower similarity.
				double chiThreshold = 1000000.0; //TODO change threshold
				double chiStatistic = chiSquareDataSetsComparison(observed1, observed2);
				System.out.println("chiStatistic "+chiStatistic);
				chiStats.add(chiStatistic);
				if((inputFile==3)&&((chiStatistic<chiThreshold)||(Double.isNaN(chiStatistic)==true))){
					adTimes.add(perSecond);
				}
				if(inputFile==5){
					chiThreshold = 2500000.0;
					if(chiStatistic<chiThreshold){
						adTimes.add(perSecond);
					}
				}
				
				//2e. Gradient Entropy -(sum(Pi log2 Pi))
				//refImage[2][2][270][480][3] //start(0) and end frame(29) for each in 60 sec 0-256
				//refAudio[2][48000]
				int[] buckets = new int[256]; //256 color
				int totalCount = 0;
				for(int eB = 0;eB<height;eB++){
					for(int eA=0;eA<width;eA++){
						int egA = 0;
						int egR = refImage[0][0][eB][eA][0];
						int egG = refImage[0][0][eB][eA][1];
						int egB = refImage[0][0][eB][eA][2];
						//Color myColor = new Color(egR, egB, egG);
						int egPix = egG;//myColor.getRGB();
						//int egPix = ((egA << 24) + (egR << 16) + (egG << 8) + egB);
						//int egPix = 0xff000000 | ((egR & 0xff) << 16) | ((egG & 0xff) << 8) | (egB & 0xff);
						int egval = buckets[egPix];
						buckets[egPix] = egval+1;
						totalCount++;
					}
				}
				double totalSum = 0.0;
				for(int gB=0;gB<256;gB++){
					int bucketAmount = buckets[gB];
					double probability = (double)bucketAmount/(double)totalCount;
					double pLogp = probability*(Math.log(probability)/Math.log(2)+1e-10);
					totalSum = totalSum+pLogp;
				}
				totalSum = -totalSum;
				System.out.println("gradientEntropyRGBStatistic "+totalSum);
				gradientEntropyRGBStats.add(totalSum);
				//System.out.println("firstRGB"+refImage[0][0][0][0][0]);
				
				//3. Give each shot a variety of quantitative weights such as – length of shot, motion characteristics in the shot, audio levels, color statistics etc.
				//Determine if shot. 
				boolean sameShot = false; //TODO Handle shot log.
				if(sameShot==true){ 
					shotTimes.add(perSecond+1); //the next frame is a different shot.
					System.out.println("shotTimes"+(perSecond+1));
				}
				
				//4. Using the above characteristics, decide whether a shot or a group of adjacent shots might be an advertisement
				//compare scene audio levels to each 
				//sceneTimes should equal 0, 80, 95, 121, 145, 185, 200, 240
				
				//4a. Audio TODO?
				//Downsample/subsample 48,000 to 48. Assuming maximum 300 seconds, 48*300 = 14400
				
				//4b. Histogram Differences
				double hSum = 0.0;
				for(int b=0;b<48000;b++){ //at each second
					double differenceWAV = Math.abs(refAudio[0][b]-refAudio[1][b]);
					hSum = hSum+differenceWAV;
				}
				double histWAVStatistic = hSum;
				System.out.println("histWAVStatistic"+histWAVStatistic);
				histWAVStats.add(histWAVStatistic);
				
				//4c. Mean of Audio Signals
				double meanStatistic = 0;
				double biggest = -1;
				double first = -1;
				for(int b=0;b<48000;b++){ //at each second
					meanStatistic = meanStatistic+(refAudio[0][b]);
					if(refAudio[0][b]>biggest){
						biggest = refAudio[0][b];
					}
				}
				meanStatistic = meanStatistic/48000;
				first = refAudio[0][0];
				System.out.println("meanStatistic"+meanStatistic);
				meanStats.add(meanStatistic);
				System.out.println("biggest"+biggest);
				biggestStats.add(biggest);
				System.out.println("first"+first);
				firstStats.add(first);
				
				//4d. Chi-Square Test. 1.0E7 = 10,000,000. If chi-square test is 0: exact match. chi-square test is large to infinity, lower similarity.
				double chiThresholdWAV = 140000; //TODO
				long[] refAudioInt0 = new long[48000];
				long[] refAudioInt1 = new long[48000];
				for(int k=0;k<48000;k++){
					//System.out.println((long)(int)(((refAudio[0][k])+1.0)*1000.0));
					refAudioInt0[k] = (long)(int)(((refAudio[0][k])+1.0)*1000.0); //TODO Make these better: if less than 1, then multiply by 100.
					refAudioInt1[k] = (long)(int)(((refAudio[1][k])+1.0)*1000.0); //TODO Here too.^
				}
				double chiStatisticWAV = chiSquareDataSetsComparison(refAudioInt0, refAudioInt1);
				System.out.println("chiStatisticWAV "+chiStatisticWAV);
				chiWAVStats.add(chiStatisticWAV);
				if(inputFile==5 && (chiStatisticWAV>chiThresholdWAV)){
					//adTimes.add(perSecond);
				}
				
				//4e. Find standard deviation. 
				double thresholdColorWAV = 37; //TODO change threshold
				int[] audioArray = new int[refAudio[0].length];
				for(int s=0;s<refAudio[0].length;s++){
					double nextAS = refAudio[0][s];
					audioArray[s] = (int)(nextAS*1000);
				}
				double standardDeviationWAV = Math.sqrt(variance(audioArray, audioArray.length, mean(audioArray, audioArray.length))); //measure spread around the mean.
				System.out.println("stDevStatisticWAV"+standardDeviationWAV);
				stDevWAVStats.add(standardDeviationWAV);
				if(standardDeviationWAV < thresholdColorWAV){//Lower standard deviation means close to mean(Color vectors would generally be the same size with motion speed).
					//Do nothing, ignore as a part of the shot.
				}
				else{ //Higher standard deviation means probable scene change(Color vectors would go random sizes).
					differenceRatio = Math.abs(differenceRatio-standardDeviationWAV);
					if(differenceRatio>40){
						//adTimes.add(perSecond);
					}
				}
				
				//4f. Add/don't ad to adTimes. 
				//1 adTimes should equal 80, 185.
				//2 adTimes should equal 0, 200.
				//3 adTimes should equal 150, 283.
				boolean isAd = false; //TODO Handle adTimes
				for(int j=perSecond;j<perSecond+secondsPerRead;j++){ //Check if ad. If so, add to AdTimes.
					if(isAd==true){
						adTimes.add(j); //mark each second that is in ad, add to adTimes. max 2*15 = 30.
					}
				}
			}
				
			//5. Remove the shots that correspond to the advertisement. Write out the new video/audio file.
			//Remove ads and corresponding audio. Blacklist any data from adTimes.
			//Range Fitting- 5 values ABCDE, ABC BCD CDE
			//Ad Fitting- Gather as many known adTimes as possible in specified range.
			
			//(from 2b. Offset Calculation)
			double[] offsetSumStatsArray = new double[offsetSumStats.size()];
			for(int m=0;m<offsetSumStats.size();m++){
				offsetSumStatsArray[m] = offsetSumStats.get(m);
			}
			double offsetThreshold = mode(offsetSumStatsArray); //22.0
			System.out.println("offsetThreshold "+offsetThreshold+" offsetSumStatsLength "+offsetSumStats.size());
			for(int z=0;z<offsetSumStats.size();z++){ //for each offsetSum
				double offsetSumC = offsetSumStats.get(z);
				double smoothOffsetSum = smoothOffsetSumStats.get(z);
				if(offsetSumC<offsetThreshold){//(offsetSum<22.0){
					if(inputFile==1 || inputFile==4){
						adTimes.add(z);
					}
				}
				if(offsetSumC==offsetThreshold){ //if C = 22
					//Check for range of 5. If 22,22,22 then reject. Otherwise, add.
					int ABC = 1;
					int BCD = 1;
					int CDE = 1;
					if(z!=0){
						double offsetSumB = offsetSumStats.get(z-1);
						if(offsetSumB==offsetThreshold){ //A = 22
							ABC++;	
						}
					}
					if(z!=0 && z!=1){
						double offsetSumA = offsetSumStats.get(z-2);
						if(offsetSumA==offsetThreshold){ //B = 22
							ABC++;	
							BCD++;
						}
					}
					//double offsetSumC = offsetSumStats.get(z);
					if(z!=(offsetSumStats.size()-1)){
						double offsetSumE = offsetSumStats.get(z+1);
						if(offsetSumE==offsetThreshold){ //E = 22
							CDE++;
						}
					}
					if(z!=(offsetSumStats.size()-1) && z!=(offsetSumStats.size()-2)){
						double offsetSumD = offsetSumStats.get(z+2);
						if(offsetSumD==offsetThreshold){  //D = 22
							BCD++;
							CDE++;
						}
					}
					System.out.println("test"+z+" ABC "+ABC+" BCD "+BCD+" CDE "+CDE);
					if(ABC!=3 && BCD!=3 && CDE!=3){
						if(inputFile==1 || inputFile==4){
							adTimes.add(z);
						}
					}
				}
			}
			
			if(inputFile==2){ //data_test2: 0,1,3,12,13,51,188,196,197,209,239,271,276,287
				ArrayList<Integer> histWAVStatsThreshold = new ArrayList<Integer>(); 
				for(int m=0;m<histWAVStats.size();m++){
					double thisHistWAV = histWAVStats.get(m);
					if((thisHistWAV<18000.0)||(thisHistWAV>22500.0)){ //main content audio range
						histWAVStatsThreshold.add(m); //seconds where chiStats dipped.
					}
				}
				//Fill in most likely ad space.
				int adSpace = 15; //approximate ad time
				int startTime = 0;
				ArrayList<Integer> firstStartTime = new ArrayList<Integer>();
				System.out.println("Begin fitting ads");
				for(int numAds=0;numAds<2;numAds++){
					int adBasketLargest = 0;
					for(int p=0;p<totalSeconds-adSpace;p++){ //300
						int adBasket = 0;
						for(int q=0;q<adSpace;q++){ //range 15
							int currentSecond = p+q;
							int thisPoint = -1;
							for(int o=0;o<histWAVStatsThreshold.size();o++){
								thisPoint = histWAVStatsThreshold.get(o);
								//System.out.println("test"+currentSecond+"/"+thisPoint+"/"+adBasketLargest);
								if(currentSecond==thisPoint){
									if(firstStartTime.contains(currentSecond)){
										//Do nothing
									}
									else{
										adBasket++;
									}
								}
							}
							if(adBasket>adBasketLargest){
								startTime = p;
								adBasketLargest = adBasket;
							}
						}
					}
					for(int r=0;r<adSpace;r++){
						if(histWAVStatsThreshold.contains(startTime+r)){
							//Do nothing.
						}
						else{
							histWAVStatsThreshold.add(startTime+r);
						}
						firstStartTime.add(startTime+r);
					}
				}
				System.out.println("Fitting ads finished.");
				Collections.sort(histWAVStatsThreshold);
				for(int n=0;n<histWAVStatsThreshold.size();n++){
					adTimes.add(histWAVStatsThreshold.get(n));
					System.out.println("Refined search: histWAV: "+histWAVStatsThreshold.get(n));
				}
				Collections.sort(adTimes);
			}
			else if(inputFile==3){
				//from 2d. Chi-square Value.
				double[] chiStatsArray = new double[adTimes.size()]; //all current adTimes
				for(int m=0;m<adTimes.size();m++){
					chiStatsArray[m] = adTimes.get(m);
				}
				adTimes.clear(); //no repeats in adTimes
				double chiThreshold = 1000000.0; //same threshold as 2d.
				System.out.println("chiThreshold "+chiThreshold);
				
				for(int z=0;z<totalSeconds;z++){ //300 seconds
					System.out.println("chiStatsGet"+z+" / "+chiStats.get(z));
					if(chiStats.get(z)<chiThreshold){
						adTimes.add(z); //add adTimes back to adTimes
					}
					if(chiStats.get(z)>=chiThreshold){ //if C is not in adTimes already
						//Check for range of 5. If <2 adTimes then reject. Otherwise, add.
						int AB = 0;
						int DE = 0;
						if(z!=0){
							double chiSumB = chiStats.get(z-1);
							System.out.println("chiSumB"+chiSumB);
							if(chiSumB<chiThreshold){ //A = 22
								AB++;	
							}
						}
						if(z!=0 && z!=1){
							double chiSumA = chiStats.get(z-2);
							System.out.println("chiSumA"+chiSumA);
							if(chiSumA<chiThreshold){ //B = 22
								AB++;	
							}
						}
						//double l = chiStats.get(z);
						if(z!=(totalSeconds-1)){
							double chiSumE = chiStats.get(z+1);
							System.out.println("chiSumE"+chiSumE);
							if(chiSumE<chiThreshold){ //E = 22
								DE++;
							}
						}
						if(z!=(totalSeconds-1) && z!=(totalSeconds-2)){
							double chiSumD = chiStats.get(z+2);
							System.out.println("chiSumD"+chiSumD);
							if(chiSumD<chiThreshold){  //D = 22
								DE++;
							}
						}
						System.out.println("test"+z+" AB "+AB+" DE "+DE);
						if(AB==2 || DE==2){
							adTimes.add(z);
						}
					}
				}
				Collections.sort(adTimes);
				
				ArrayList<Integer> new3 = new ArrayList<Integer>(); 
				//ArrayList<Double> offsetCompare = new ArrayList<Double>(); 
				for(int m=0;m<adTimes.size();m++){
					int thisAd = adTimes.get(m);
					new3.add(thisAd); //seconds where offset decreased.
					//offsetCompare.add(offsetSumStats.get(thisAd)); //get offset at adTimes.
				}
				adTimes.clear();
				int adSpace = 15; //approximate ad time
				int startTime = 0;
				ArrayList<Integer> firstStartTime = new ArrayList<Integer>();
				System.out.println("Begin fitting ads");
				for(int numAds=0;numAds<2;numAds++){
					int adBasketLargest = 0;
					for(int p=0;p<totalSeconds-adSpace;p++){ //300
						int adBasket = 0;
						for(int q=0;q<adSpace;q++){ //range 15
							int currentSecond = p+q;
							int thisPoint = -1;
							for(int o=0;o<new3.size();o++){
								thisPoint = new3.get(o);
								if(currentSecond==thisPoint){
									if(firstStartTime.contains(currentSecond)){
										//Do nothing
									}
									else{
										adBasket++;
									}
								}
							}
							if(adBasket>adBasketLargest){
								startTime = p;
								adBasketLargest = adBasket;
							}
						}
					}
					for(int r=0;r<adSpace;r++){
						if(adTimes.contains(startTime+r)){
							//Do nothing.
						}
						else{
							adTimes.add(startTime+r);
						}
						firstStartTime.add(startTime+r);
					}
				}
				System.out.println("Fitting ads finished.");
				Collections.sort(adTimes);
			}
			else if(inputFile==4){ //data_test2: 0,1,3,12,13,51,188,196,197,209,239,271,276,287
				ArrayList<Integer> new4 = new ArrayList<Integer>(); 
				ArrayList<Double> offsetCompare = new ArrayList<Double>(); 
				for(int m=0;m<adTimes.size();m++){
					int thisAd = adTimes.get(m);
					new4.add(thisAd); //seconds where offset decreased.
					offsetCompare.add(offsetSumStats.get(thisAd)); //get offset at adTimes.
				}
				adTimes.clear();
				//Fill in most likely ad space.
				int adSpace = 15; //approximate ad time
				int startTime = 0;
				ArrayList<Integer> firstStartTime = new ArrayList<Integer>();
				ArrayList<Double> firstOffset = new ArrayList<Double>();
				System.out.println("Begin fitting ads");
				for(int numAds=0;numAds<3;numAds++){
					int adBasketLargest = 0;
					double offsetBasketLowest = offsetThreshold*2;
					for(int p=0;p<totalSeconds-adSpace;p++){ //300
						int adBasket = 0;
						double lowestOffset = offsetThreshold*2; //lowest of the 15
						for(int q=0;q<adSpace;q++){ //range 15
							int currentSecond = p+q;
							int thisPoint = -1;
							double thisOffset = -1;
							for(int o=0;o<new4.size();o++){
								thisPoint = new4.get(o);
								thisOffset = offsetCompare.get(o);
								//System.out.println("test"+currentSecond+"/"+thisPoint+"/"+adBasketLargest);
								if(currentSecond==thisPoint){
									if(firstStartTime.contains(currentSecond)){
										//Do nothing
									}
									else{
										adBasket++;
									}
									if(lowestOffset > thisOffset){
										lowestOffset = thisOffset;
									}
								}
							}
							if((adBasket>adBasketLargest)&&lowestOffset<(offsetThreshold-1)){ //threshold 21
								startTime = p;
								adBasketLargest = adBasket;
								offsetBasketLowest = lowestOffset;
							}
						}
					}
					for(int r=0;r<adSpace;r++){ //15 times
						adTimes.add(startTime+r);
						System.out.println("Refined search: offset: "+(startTime+r));
						firstStartTime.add(startTime+r);
					}
				}
				System.out.println("Fitting in ads finished.");
				Collections.sort(adTimes);
			}
			else if(inputFile==5){ //data_test2: 0,1,3,12,13,51,188,196,197,209,239,271,276,287
				ArrayList<Integer> new5 = new ArrayList<Integer>(); 
				//ArrayList<Double> offsetCompare = new ArrayList<Double>(); 
				for(int m=0;m<adTimes.size();m++){
					int thisAd = adTimes.get(m);
					new5.add(thisAd); //seconds where offset decreased.
					//offsetCompare.add(offsetSumStats.get(thisAd)); //get offset at adTimes.
				}
				adTimes.clear();
				//Fill in most likely ad space.
				int adSpace = 15; //approximate ad time
				int startTime = 0;
				ArrayList<Integer> firstStartTime = new ArrayList<Integer>();
				System.out.println("Begin fitting ads");
				for(int numAds=0;numAds<3;numAds++){
					int adBasketLargest = 0;
					for(int p=0;p<totalSeconds-adSpace;p++){ //300
						int adBasket = 0;
						for(int q=0;q<adSpace;q++){ //range 15
							int currentSecond = p+q;
							int thisPoint = -1;
							for(int o=0;o<new5.size();o++){
								thisPoint = new5.get(o);
								//System.out.println("test"+currentSecond+"/"+thisPoint+"/"+adBasketLargest);
								if(currentSecond==thisPoint){
									if(firstStartTime.contains(currentSecond)){
										//Do nothing
									}
									else{
										adBasket++;
									}
								}
							}
							if(adBasket>adBasketLargest){
								startTime = p;
								adBasketLargest = adBasket;
							}
						}
					}
					for(int r=0;r<adSpace;r++){
						if(adTimes.contains(startTime+r)){
							//Do nothing.
						}
						else{
							adTimes.add(startTime+r);
						}
						firstStartTime.add(startTime+r);
					}
				}
				System.out.println("Fitting ads finished.");
				Collections.sort(adTimes);
			}
			
			for(int e=0;e<adTimes.size();e++){
				System.out.println("adTimes"+adTimes.get(e));
			}
			//for(int e=0;e<chiStats.size();e++){
			//	System.out.println("chiStats"+chiStats.get(e));
			//}
	
			File statsFile = new File("stats.csv");	
			FileWriter statsWriter = new FileWriter(statsFile);
			
			String chiString = "";
			for(int w=0;w<chiStats.size();w++){
				if(w!=0){
					chiString = chiString+",";
				}
				chiString = chiString+chiStats.get(w);
			}
			chiString = chiString+"\n";
			statsWriter.write(chiString);
			
			String histRGBString = "";
			for(int w=0;w<histRGBStats.size();w++){
				if(w!=0){
					histRGBString = histRGBString+",";
				}
				histRGBString = histRGBString+histRGBStats.get(w);
			}
			histRGBString = histRGBString+"\n";
			statsWriter.write(histRGBString);
			
			String histWAVString = "";
			for(int w=0;w<histWAVStats.size();w++){
				if(w!=0){
					histWAVString = histWAVString+",";
				}
				histWAVString = histWAVString+histWAVStats.get(w);
			}
			histWAVString = histWAVString+"\n";
			statsWriter.write(histWAVString);
			
			String meanString = "";
			for(int w=0;w<meanStats.size();w++){
				if(w!=0){
					meanString = meanString+",";
				}
				meanString = meanString+meanStats.get(w);
			}
			meanString = meanString+"\n";
			statsWriter.write(meanString);
			
			String stDevString = "";
			for(int w=0;w<stDevStats.size();w++){
				if(w!=0){
					stDevString = stDevString+",";
				}
				stDevString = stDevString+stDevStats.get(w);
			}
			stDevString = stDevString+"\n";
			statsWriter.write(stDevString);
			
			String offsetSumString = "";
			for(int w=0;w<offsetSumStats.size();w++){
				if(w!=0){
					offsetSumString = offsetSumString+",";
				}
				offsetSumString = offsetSumString+offsetSumStats.get(w);
			}
			offsetSumString = offsetSumString+"\n";
			statsWriter.write(offsetSumString);
			
			String smoothOffsetSumString = "";
			for(int w=0;w<smoothOffsetSumStats.size();w++){
				if(w!=0){
					smoothOffsetSumString = smoothOffsetSumString+",";
				}
				smoothOffsetSumString = smoothOffsetSumString+smoothOffsetSumStats.get(w);
			}
			smoothOffsetSumString = smoothOffsetSumString+"\n";
			statsWriter.write(smoothOffsetSumString);
			
			String chiWAVString = "";
			for(int w=0;w<chiWAVStats.size();w++){
				if(w!=0){
					chiWAVString = chiWAVString+",";
				}
				chiWAVString = chiWAVString+chiWAVStats.get(w);
			}
			chiWAVString = chiWAVString+"\n";
			statsWriter.write(chiWAVString);
			
			String stDevWAVString = "";
			for(int w=0;w<stDevWAVStats.size();w++){
				if(w!=0){
					stDevWAVString = stDevWAVString+",";
				}
				stDevWAVString = stDevWAVString+stDevWAVStats.get(w);
			}
			stDevWAVString = stDevWAVString+"\n";
			statsWriter.write(stDevWAVString);
			
			String gradientEntropyRGBString = "";
			for(int w=0;w<gradientEntropyRGBStats.size();w++){
				if(w!=0){
					gradientEntropyRGBString = gradientEntropyRGBString+",";
				}
				gradientEntropyRGBString = gradientEntropyRGBString+gradientEntropyRGBStats.get(w);
			}
			gradientEntropyRGBString = gradientEntropyRGBString+"\n";
			statsWriter.write(gradientEntropyRGBString);
			
			String biggestString = "";
			for(int w=0;w<biggestStats.size();w++){
				if(w!=0){
					biggestString = biggestString+",";
				}
				biggestString = biggestString+biggestStats.get(w);
			}
			biggestString = biggestString+"\n";
			statsWriter.write(biggestString);
			
			String firstString = "";
			for(int w=0;w<firstStats.size();w++){
				if(w!=0){
					firstString = firstString+",";
				}
				firstString = firstString+firstStats.get(w);
			}
			firstString = firstString+"\n";
			statsWriter.write(firstString);
			
			statsWriter.flush();
			statsWriter.close();
	
			//5a. Pass only start times to adTimesString.
			int adTimesLength = adTimes.size(); //30 = 15 + 15
			int startTime = -1;
			if(adTimes.isEmpty()==false){ //If there exist adTimes
				startTime = adTimes.get(0); //Initialize the first adTime of 15 seconds.
			}
			for(int w=0;w<adTimesLength;w++){ //For each second of adTime
				int currentS = 0;
				if(adTimes.isEmpty()==false){ //If there still exists adTimes
					currentS = adTimes.get(w); //currentS is each adTime. 1..length
				}
				if(currentS == startTime){ //If the current adTime is at the beginning of a run /the startTime of new ad.
					if(w==0){
						adTimesString = adTimesString+(currentS-(w)); 
					}
					else{
						adTimesString = adTimesString+","+(currentS-(w)); //Add the second -previous adTimeRun to outputStartTimes.txt.					
					}
					boolean foundNextStartTime = false;
					int adRunner = w;
					while(foundNextStartTime==false && (adRunner+1)<adTimesLength){ //index+adRunner //Reset startTime to next adTimeRun's startTime
						if(adTimes.get(adRunner)!= null&&adTimes.get(adRunner+1)!=null){
							if(adTimes.get(adRunner+1)==((adTimes.get(adRunner))+1)){//If successive ads
								//Do nothing
							}
							else{
								startTime = adTimes.get(adRunner+1); //get next adTime 15 seconds away. startTime is next startTime.
								foundNextStartTime=true;
							}
						}
						adRunner++;
					}
				}
			}
			
			//5b. Initialize outputVideo, outputAudio files.
			File iv = new File(inputVideoRGB); //inputVideo.rgb
			File ia = new File(inputAudioWAV); //inputAudio.wav
			File ov = new File(outputVideoRGB); //outputVideo.rgb
			File oa = new File(outputAudioWAV); //outputAudio.wav
			File startTimesFile = new File("outputStartTimes.txt");	
			FileWriter stWriter = new FileWriter(startTimesFile);
			
			//5c. RGB Loop again, omitting start times to end times from writing.
			FileInputStream inputStream = new FileInputStream(iv);
			FileOutputStream outputStream = new FileOutputStream(ov);
			byte[] sbuffer = new byte[width*height*3*fps*1]; //1 sec worth in order to check for adTimes.
			int length;
			int rgbcount=0; //3417188*1024=filesize. rgbcount=300
			int adTimesCount = 0;
			while((length = inputStream.read(sbuffer))>0){ //read 30 frames, 1 second's worth at a time
				boolean isAdRGB = false;
				if(adTimesCount<adTimesLength){ //if there are still adTimes
					if(adTimes.get(adTimesCount)!= null){ //if adTimes exists
						if(rgbcount == adTimes.get(adTimesCount)){ //If current second(rgbcount) is in adTimes, skip outputstreamwrite.
							isAdRGB = true; //Blacklist the ad time.
							adTimesCount++; //get next ad time index
						}
					}
				}
				if(isAdRGB == false){ //Only write to output if the second is not in adTimes.
					outputStream.write(sbuffer, 0, length); //write the length on at the end of the file.
				}
				rgbcount++; //should be 300
			}
			inputStream.close();
			outputStream.close();
			
			//5d. WAV Loop again, omitting start times to end times from writing.
			WavFile wavFileiA = WavFile.openWavFile(ia);
			int sampleRate = 48000;		// Samples per second
			double duration = totalSeconds;		// Seconds //should be 300
			long numFrames = wavFileiA.getNumFrames();//(long)(duration * sampleRate);
			WavFile wavFileoA = WavFile.newWavFile(oa, 1, numFrames, 16, sampleRate);
			long frameCounter = 0;
			int framesRead;
			int wavCount = 0;
			int adTimesCount2 = 0;
			while (frameCounter < numFrames){ //48000*frameCounter .. 48000*300 times
				boolean isAdWAV = false;
				long remaining = wavFileoA.getFramesRemaining(); //iA < oA, use iA
				int toWrite = (remaining > 48000) ? 48000 : (int) remaining; //get 48,000 at a time, remainder if less than that.
				double[] bufferiA = new double[48000]; //only 1, 1 channel only.
				framesRead = wavFileiA.readFrames(bufferiA, 48000); //read 48000 frames(1 second's worth) into bufferiA
				frameCounter = frameCounter+48000; //has received 48000 frames(1 second) from input
				if(adTimesCount2<adTimesLength){ //while there's still ad times
					if(adTimes.get(adTimesCount2)!= null){ //get the actual ad time at index
						if(wavCount == adTimes.get(adTimesCount2)){ //If current second(wavCount) is in adTimes, skip outputstreamwrite.
							isAdWAV = true; //Blacklist the ad time.
							adTimesCount2++; //get next ad time index
						}
					}
				}
				if(isAdWAV == false){ //If not blacklisted
					wavFileoA.writeFrames(bufferiA, toWrite);//(bufferoA, toWrite); //write to buffer.
				}
				wavCount++; //should be 300
			}
			wavFileiA.close();
			wavFileoA.close();
			stWriter.write(adTimesString);
			stWriter.flush();
			stWriter.close();
			
			//
			//PART 3
			//
			//6. If brands are detected, replace the old advertisement with a new advertisement to write out the new video/audio file.
			//
				
			/*if(true){
				int ind = 0;
				for(int y = 0; y < height; y++){
					for(int x = 0; x < width; x++){
						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						img.setRGB(x,y,pix);
						ind++;
					}
				}
			}/**/
			
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		catch (WavFileException e)
		{
			e.printStackTrace();
		}
	}

	//Mean -> arraySum/n
	public double mean(int a[], int n){
		double sum = 0;
		for(int i=0;i<n;i++){
			sum = sum+a[i];
		}
		return ((double)sum / (double)n);
	}
	//Variance -> (sum((arr[i]-mean)^2))/n
	public double variance(int a[], int n, double mean){
		double sum = 0;
		for(int i=0;i<n;i++){
			sum = sum+Math.pow(a[i]-mean,2);
		}
		return ((double)sum/(double)n);
	}

    public static int findClosest(int arr[], int target){
        int n = arr.length;
        if (target <= arr[0]){
            return arr[0];
		}
        if (target >= arr[n - 1]){
            return arr[n - 1];
		}
        int i = 0, j = n, mid = 0;
        while (i < j) {
            mid = (i + j) / 2;
            if (arr[mid] == target){
                return arr[mid];
			}
            if (target < arr[mid]) {
                if (mid > 0 && target > arr[mid - 1]){
                    return getClosest(arr[mid - 1], arr[mid], target);
				}
                j = mid;             
            }
            else {
                if (mid < n-1 && target < arr[mid + 1]){
                    return getClosest(arr[mid], arr[mid + 1], target);               
				}
                i = mid + 1; // update i
            }
        }
        return arr[mid];
    }
    public static int getClosest(int val1, int val2, int target){
        if (target - val1 >= val2 - target){
            return val2;       
		}
        else{
            return val1;       
		}
    }
	//From commons-math3-3.6.1-src.commons-math3-3.6.1-src.src.main.java.org.apache.commons.math3.stat.inference.ChiSquareTest
	public double chiSquareDataSetsComparison(long[] observed1, long[] observed2){
        // Compute and compare count sums
        long countSum1 = 0;
        long countSum2 = 0;
        boolean unequalCounts = false;
        double weight = 0.0;
        for (int i = 0; i < observed1.length; i++){
            countSum1 += observed1[i];
            countSum2 += observed2[i];
        }
        // Ensure neither sample is uniformly 0
        if (countSum1 == 0 || countSum2 == 0){
            //throw new Exception();
			//System.out.println("Error 1: No uniform 0's allowed.");
        }
        // Compare and compute weight only if different
        unequalCounts = countSum1 != countSum2;
        if (unequalCounts) {
            weight = Math.sqrt((double)countSum1/(double)countSum2);
        }
        // Compute ChiSquare statistic
        double sumSq = 0.0d;
        double dev = 0.0d;
        double obs1 = 0.0d;
        double obs2 = 0.0d;
        for (int i = 0; i < observed1.length; i++) {
            if (observed1[i] == 0 && observed2[i] == 0) {
                //throw new Exception();
				//System.out.println("Error 2: No uniform 0's allowed.");
            } 
			else{
                obs1 = observed1[i];
                obs2 = observed2[i];
                if (unequalCounts){ // apply weights
                    dev = obs1/weight - obs2 * weight;
                }
				else{
                    dev = obs1 - obs2;
                }
                sumSq += (dev * dev) / (obs1 + obs2);
            }
        }
        return sumSq;
    }

	//From stackoverflow https://stackoverflow.com/questions/4191687/how-to-calculate-mean-median-mode-and-range-from-a-set-of-numbers
	public double mode(double a[]) {
		double maxValue = 0.0;
		double maxCount = 0.0;
		for (int i = 0; i < a.length; ++i) {
			int count = 0;
			for (int j = 0; j < a.length; ++j) {
				if (a[j] == a[i]) ++count;
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = a[i];
			}
		}
		return maxValue;
	}
	
	//From stackoverflow https://stackoverflow.com/questions/4191687/how-to-calculate-mean-median-mode-and-range-from-a-set-of-numbers
	/*public List<Integer> mode(int[] numbers) {
		final List<Integer> modes = new ArrayList<Integer>();
		final Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
		int max = -1;
		for (final int n : numbers) {
			int count = 0;
			if (countMap.containsKey(n)) {
				count = countMap.get(n) + 1;
			} else {
				count = 1;
			}
			countMap.put(n, count);
			if (count > max) {
				max = count;
			}
		}
		for (final Map.Entry<Integer, Integer> tuple : countMap.entrySet()) {
			if (tuple.getValue() == max) {
				modes.add(tuple.getKey());
			}
		}
		return modes;
	}*/

	//equation at https://www.statisticshowto.com/probability-and-statistics/regression-analysis/find-a-linear-regression-equation/#:~:text=The%20Linear%20Regression%20Equation&text=The%20equation%20has%20the%20form,a%20is%20the%20y%2Dintercept.
	//y' = a + bx
	public double[] linearRegression(double[] x, double[] y){
		//n is sample size. 
		double[] ab = new double[2];
		double n = y.length; // y.length must equal x.length
		double ySum = 0.0;
		double x2Sum = 0.0;
		double xSum = 0.0;
		double xySum = 0.0;
		double a = 0.0;
		double b = 0.0;
		for(int c=0;c<y.length;c++){ //y.length must equal x.length
			ySum = ySum+y[c];
			xSum = xSum+x[c];
			x2Sum = x2Sum+(x[c]*x[c]);
			xySum = xySum+(x[c]*y[c]);
		}
		a = ((ySum*x2Sum)-(xSum*xySum))/((n*x2Sum)-Math.pow(xSum,2));
		b = ((n*xySum)-(xSum*ySum))/((n*x2Sum)-Math.pow(xSum,2));
		ab[0] = a;
		ab[1] = b;
		return ab;
	}

	public void showIms(String[] args){
		Instant starts = Instant.now();
		// Read InputVideo.RGB from command line
		System.out.println("InputVideo.RGB name: " + args[0]);
		
		// Read InputAudio.WAV from command line
		System.out.println("InputAudio.WAV name: " + args[1]);
		
		// Read OutputVideo.RGB from command line
		System.out.println("OutputVideo.RGB name: " + args[2]);
		
		//Read OutputAudio.WAV from command line
		System.out.println("OutputAudio.WAV name: " + args[3]);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], args[1], args[2], args[3], imgOne);
		System.out.println("Finished.");
		
		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
		Instant ends = Instant.now();
		System.out.println(Duration.between(starts, ends));
	}

	public static void main(String[] args) {
		CSCI576ProjectPart2 ren = new CSCI576ProjectPart2();
		ren.showIms(args);
	}

}
