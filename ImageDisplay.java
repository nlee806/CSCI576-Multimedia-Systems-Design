
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 512; // default image width and height
	int height = 512;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img, float paramS, int paramQ, int paramM)
	{
		try
		{
			//Initial Image Data 256x256
			int frameLength = width*height*3;
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);
			long len = frameLength;
			byte[] bytes = new byte[(int) len];
			raf.read(bytes);
			//
			
			//1. Iterate through original image, collecting RGB values in 3D array.
			byte[][][] refImage = new byte[height][width][3];
			int refInd = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte rA = 0;
					byte rR = bytes[refInd];
					refImage[y][x][0] = rR;
					byte rG = bytes[refInd+height*width];
					refImage[y][x][1] = rG;
					byte rB = bytes[refInd+height*width*2];
					refImage[y][x][2] = rB;
					refInd++;
				}
			}
			
			//2. Quantization: 2^paramQ values per channel, 0-255 into vpc sections.
			double vpc = Math.pow(2,paramQ);
			List<Double> midpointsList = new ArrayList<>();
					
			if(paramM<0){ //2a. Mode: Uniform Scaling
				double rangeSize = 256/vpc;
				double midpoint = rangeSize/2;
				for(double z=midpoint;z<256;z+=rangeSize){
					midpointsList.add(z);
				}
				Object[] midpoints = midpointsList.toArray();
				for(int u = 0; u < height; u++){
					for(int t = 0; t < width; t++){
						for(int w = 0; w < 3; w++){
							double quantizedValue = 0;
							double smallestDistance = 300; //Larger than 255
							double searchValue = refImage[u][t][w] & 0xff;
							for(int v = 0; v < midpoints.length; v++){
								double distance = Math.abs((double)midpoints[v] - searchValue);
								if(distance < smallestDistance){
									smallestDistance = distance;
									quantizedValue = (double)midpoints[v];
								}
							}
							int IntQV = (int)quantizedValue;
							refImage[u][t][w] = (byte)IntQV;//(IntQV & 0xff);
						}
					}
				}
			}
			else{ //2b. Mode: Logarithmic Scaling
				int pivot = paramM; //any integer from 0-255
				double higherThanPivot = Math.abs(255-paramM);
				double numHighPartitions = Math.round(higherThanPivot/255 * vpc); // XXX should not exceed 8
				double lowerThanPivot = pivot;
				double numLowPartitions = Math.round(lowerThanPivot/255 * vpc); // XXX should not exceed 8
				
				double lowPowKeep = 0;
				for(double lowZ = 0; lowZ<numLowPartitions;lowZ++){
					double lowMidPoint = pivot-Math.pow(2,lowZ); //Alternatively keep adding lowPowKeep to Math.pow(2,lowZ)
					lowPowKeep = Math.pow(2,lowZ);
					midpointsList.add(lowMidPoint);
				}
				double highPowKeep = 0;
				for(double highZ = 0; highZ<numHighPartitions; highZ++){
					double highMidPoint = pivot+Math.pow(2,highZ); //Alternatively keep adding highPowKeep to Math.pow(2,highZ)
					highPowKeep = Math.pow(2,highZ);
					midpointsList.add(highMidPoint);
				}
				Object[] midpoints = midpointsList.toArray();
				double max = (double) midpoints[0];
				double min = (double) midpoints[0];
				for(int o=0;o<midpoints.length;o++){
					if((double)midpoints[o]>max){
						max = (double)midpoints[o];
					}
					if((double)midpoints[o]<min){
						min = (double)midpoints[o];
					}
				}
				//Scale midpoints to 0-255.
				for(int p=0;p<midpoints.length;p++){
					double unscaled = (double)midpoints[p];
					double scaled = (255-0)*(unscaled-min)/(max-min)+0;
					midpoints[p] = scaled;
				}
				
				for(int u = 0; u < height; u++){
					for(int t = 0; t < width; t++){
						for(int w = 0; w < 3; w++){
							double quantizedValue = 0;
							double smallestDistance = 300; //Larger than 255
							double searchValue = refImage[u][t][w] & 0xff;
							for(int v = 0; v < midpoints.length; v++){
								double distance = Math.abs((double)midpoints[v] - searchValue);
								if(distance < smallestDistance){
									smallestDistance = distance;
									quantizedValue = (double)midpoints[v];
								}
							}
							int IntQV = (int)quantizedValue;
							refImage[u][t][w] = (byte)IntQV;//(IntQV & 0xff);
						}
					}
				}		
			}
			
			//3. Scale: Iterate through new image, making new x,y positions.
			int height_new = (int)(paramS*height);
			int width_new = (int)(paramS*width);
			double stepSize = 1.0/paramS;
			int x_new = 0;
			int y_new = 0;

			for(double sY = 0; sY < height; sY+=stepSize)
			{
				x_new = 0;
				for(double sX = 0; sX < width; sX+=stepSize)
				{
					int y = (int)Math.floor(sY);
					int x = (int)Math.floor(sX);
					
					byte a = 0;
					byte r = refImage[y][x][0]; //= bytes[ind];
					byte g = refImage[y][x][1]; //= bytes[ind+height*width];
					byte b = refImage[y][x][2]; //= bytes[ind+height*width*2];
					//8 bits r,8 bits g,8 bits b,512x512
					
					//3a. Average Filtered Lookup, Kernel
					int filteredR = 0;
					int filteredG = 0;
					int filteredB = 0;
					for(int q=0;q<=2;q++){
						int origPixRGB = refImage[y][x][q];
						List<Integer> lookupList = new ArrayList<>();
					
						if(y!=0&&x!=0){
							if(Objects.isNull(refImage[y-1][x-1][q])!=true){
								int oA = refImage[y-1][x-1][q] & 0xff;
								lookupList.add(oA);
							}
						}
						if(y!=0){
							if(Objects.isNull(refImage[y-1][x][q])!=true){
								int oB = refImage[y-1][x][q] & 0xff;
								lookupList.add(oB);
							}
						}
						if(y!=0 && x<width-1){
							if(Objects.isNull(refImage[y-1][x+1][q])!=true){
								int oC = refImage[y-1][x+1][q] & 0xff;
								lookupList.add(oC);
							}
						}
						if(x!=0){
							if(Objects.isNull(refImage[y][x-1][q])!=true){
								int oD = refImage[y][x-1][q] & 0xff;
								lookupList.add(oD);
							}
						}
						if(Objects.isNull(refImage[y][x][q])!=true){
							int oE = refImage[y][x][q] & 0xff;
							lookupList.add(oE);
						}
						if(x<width-1){
							if(Objects.isNull(refImage[y][x+1][q])!=true){
								int oF = refImage[y][x+1][q] & 0xff;
								lookupList.add(oF);
							}
						}
						if(y<height-1 && x!=0){
							if(Objects.isNull(refImage[y+1][x-1][q])!=true){
								int oG = refImage[y+1][x-1][q] & 0xff;
								lookupList.add(oG);
							}
						}
						if(y<height-1){
							if(Objects.isNull(refImage[y+1][x][q])!=true){
								int oH = refImage[y+1][x][q] & 0xff;
								lookupList.add(oH);
							}
						}
						if(y<height-1 && x<width-1){
							if(Objects.isNull(refImage[y+1][x+1][q])!=true){
								int oI = refImage[y+1][x+1][q] & 0xff;
								lookupList.add(oI);
							}
						}
						int lookupListSize = lookupList.size();
						double filterSum = 0;
						for(int value:lookupList){
							double filter = (double)(1.0/lookupListSize)*(value);
							filterSum = filterSum+filter;
						}
						if(q==0){
							filteredR = (int) filterSum;
						}
						else if(q==1){
							filteredG = (int) filterSum;
						}
						else if(q==2){
							filteredB = (int) filterSum;
						}
					}
					byte r_new = (byte)filteredR;
					byte g_new = (byte)filteredG;
					byte b_new = (byte)filteredB;
					//3b. No Quantization for 8 bits
					if(paramQ==8){
						r_new = r;
						g_new = g;
						b_new = b;
					}
					//

					//int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					int filterPix = 0xff000000 | ((r_new & 0xff) << 16) | ((g_new & 0xff) << 8) | (b_new & 0xff);
					
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					if(x_new<width_new && y_new<height_new){
						img.setRGB(x_new,y_new,filterPix); //img.setRGB(x,y,pix);
					}
					x_new++;
				}
				y_new++;
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){
		
		float paramS;
		int paramQ;
		int paramM;
		
		// Read Scale from command line; between 0.0 < s <= 1.0
		paramS = Float.parseFloat(args[1]);
		System.out.println("Scale: " + paramS);
		
		// Read Quantization from command line; between 1 <= q <= 8
		paramQ = Integer.parseInt(args[2]);
		System.out.println("Quantization: " + paramQ);
		
		// Read Mode from command line; (-) -1 uniform quantization, (0,+) 0-255 logarithmic quantization
		paramM = Integer.parseInt(args[3]);
		System.out.println("Mode: " + paramM);
		
		int height_new = (int)(paramS*height);
		int width_new = (int)(paramS*width);

		// Read in the specified image
		imgOne = new BufferedImage(width_new, height_new, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne, paramS, paramQ, paramM);
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
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
