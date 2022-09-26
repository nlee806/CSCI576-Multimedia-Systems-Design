
Part 2. Ad Detection and Removal

--I. Program Compilation/Execution--

javac --release 8 CSCI576ProjectPart2.java
(may have to do java --release 8 CSCI576ProjectPart2.java WavFile.java WavFileException.java)

Accepted Inputs: 
java CSCI576ProjectPart2 data_test1.rgb data_test1.wav output1.rgb output1.wav
java CSCI576ProjectPart2 data_test2.rgb data_test2.wav output1.rgb output1.wav
java CSCI576ProjectPart2 data_test3.rgb data_test3.wav output1.rgb output1.wav
java CSCI576ProjectPart2 test1.rgb test1_mono.wav output1.rgb output1.wav
java CSCI576ProjectPart2 test2.rgb test2.wav output1.rgb output1.wav

In part 5 it may speed up the program to comment out unused inputFiles(if inputFile==(number of file not being used)).

Outputs: 
output1.rgb and output1.wav, files without ads
stats.csv, all statistics captured for review. 

--II. Overview Statistics/Thresholds Calculation--

1. Collect all data from rgb, wav files
2a. High Pass Filtering to accentuate high frequency areas(ads tend to have more color/motion).
2b. Color space analysis, color matching. 
	Calculates speed vectors of color-matched blocks in image using pythagorean theorem.
	(Generally most vectors should move at same speed if in same shot. 
	If not same shot, random vectors because little to no correlation between colors of one scene and another.)
	(Standard DeviationRGB). how much vectors vary from mean. 
	(Offset SumRGB). sum(n - (n-1)) speed vectors, difference between ordered vectors.
2c. (Histogram DifferencesRGB). compare each color space to next frame's color space for similarity.
2d. (Chi-Square TestRGB). Use chi-square formula. If p-value is 0, perfect match. p->Infinity, less similar match.
2e. (Gradient EntropyRGB). entropy formula for RGB value.
4b. (Histogram DifferencesWAV). compare each audio values to next frame's audio values for similarity
4c. (MeanWAV). Mean of audio signals. Average audio may be significantly higher than main content.
    (BiggestWAV). Largest value of audio signals. Audio may be significantly higher than main content.
    (FirstWAV). First value of frame. If ad, audio may differ significantly higher/lower from main content.
4d. (Chi-Square TestWAV). 2d, but for audio WAV file.
4e. (Standard Deviation WAV). 2b., but for audio WAV file.

Different statistics/thresholds combinations are used based on the data, which varies in video/audio.
To graph statistics, run CSCI576ProjectPart2.ipynb with the outputted stats.csv file to see statistics plotted.
The ProjectGraphs png files are pictures of all the statistic graphs.
