package com.github.tsiangleo.sr.client.preprocess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by tsiang on 2017/2/17.
 */

public class PreprocessUtilOnAndroid {


	/**
	 * 获取一个wav文件对应的语谱图的Bitmap.
	 * @param fftlen
	 * @param hopsize
	 * @param wavFile
	 * @return
     * @throws Exception
     */
	public static Bitmap getBitmap(int fftlen,int hopsize,File wavFile) throws Exception {
		int[][] pixel = getPixelMatrix(wavFile, fftlen,hopsize);
		int[][] pixel128 = get128PixelMatrix(pixel);
		return createBitmap(pixel128);
	}

	/**
	 * 前53列是黑的，不要
	 * @param pixel
	 * @return
	 */
	public static Bitmap getBitmapUpdate(int fftlen,int hopsize,File wavFile) throws Exception {
		int[][] pixel = getPixelMatrix(wavFile, fftlen,hopsize);
		int[][] pixel128 = get128PixelMatrix(pixel);
		return createBitmapUpdate(pixel128);
	}

	/**
	 * 获取一个wav文件对应的语谱图的Bitmap.
	 * @param fftlen
	 * @param hopsize
	 * @param wavFile
	 * @param width
	 * @return
     * @throws Exception
     */
	public static List<Bitmap> getBitmap(int fftlen, int hopsize, File wavFile,int width) throws Exception {
		int[][] pixel = getPixelMatrix(wavFile, fftlen,hopsize);
		int[][] pixel128 = get128PixelMatrix(pixel);
		return createBitmap(pixel128,width);
	}

    private static Bitmap createBitmap(int[][] pixel){
        int rows = pixel.length;    //height
        int cols = pixel[0].length; //width

        Bitmap bitmap = Bitmap.createBitmap (cols,rows, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int d = pixel[i][j];
                //（24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
                // ARGB
                int argb = Color.argb(255,d,d,d);
                bitmap.setPixel(j,i,argb);
            }
        }
        return bitmap;
    }

	/**
	 * 前53列是黑的，不要
	 * @param pixel
	 * @return
     */
	private static Bitmap createBitmapUpdate(int[][] pixel){
		int rows = pixel.length;    //height
		int cols = pixel[0].length; //width

		Bitmap bitmap = Bitmap.createBitmap (cols,rows, Bitmap.Config.ARGB_8888);

		for (int i = 0; i < rows; i++) {
			for (int j = 53; j < cols; j++) {
				int d = pixel[i][j];
				//（24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
				// ARGB
				int argb = Color.argb(255,d,d,d);
				bitmap.setPixel(j,i,argb);
			}
		}
		return bitmap;
	}

	private static List<Bitmap> createBitmap(int[][] pixel,int width){

		List<Bitmap> results = new ArrayList<>();
		int rows = pixel.length;    //height
		int cols = pixel[0].length; //width

		int numOfBitmap = cols / width;

		for (int c = 0;c<numOfBitmap;c++) {

			Bitmap bitmap = Bitmap.createBitmap (width,rows, Bitmap.Config.ARGB_8888);

			for (int j = 0; j < width; j++) {

				for (int i = 0; i < rows; i++) {

					int d = pixel[i][j + c * width];
					//（24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
					// ARGB
					int argb = Color.argb(255, d, d, d);
					bitmap.setPixel(j, i, argb);
				}
			}
			results.add(bitmap);
		}
		return results;
	}

    /**
	 * 获取像素矩阵的值。每一帧的数据用一列来表示。
	 * @param wavfile
	 * @param fftlen
	 * @param hopsize
	 * @return  M行N列的矩阵。M等于fftlen/2+1。N等于总的帧数。
	 * @throws Exception
	 */
	private static int[][] getPixelMatrix(File wavfile, int fftlen,int hopsize)
			throws Exception {
		int[] samples = loadWav(wavfile);
		STFT3 stft = new STFT3(fftlen, hopsize, samples.length);

		double[][] db = stft.feedData(samples, samples.length);

		int width = db.length;
		int height = db[0].length;

		int[][] pixel = new int[height][width];

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int d = (int) db[i][j];
				pixel[j][i] = d;
			}
		}
		return pixel;
	}
	

	/**
	 * 每列只获取128个元素。
	 * @param origin
	 * @return
	 * @throws Exception
	 */
	private static int[][] get128PixelMatrix(int[][] origin){

		int cols = origin[0].length;

		int[][] newpixel = new int[128][cols];

		for (int i = 0; i < 128; i++) {
			for (int j = 0; j < cols; j++) {
				int d =  origin[i+1][j];
				newpixel[i][j] = d;
			}
		}
		return newpixel;
	}
	/**
	 * 读取wav文件数据。
	 * 
	 * @param wavfile
	 * @return
	 * @throws IOException
	 */
	private static int[] loadWav(File wavfile) throws Exception {

		WavFile wavFile = WavFile.openWavFile(wavfile);
//		wavFile.display();
		if (wavFile.getNumFrames() > Integer.MAX_VALUE)
			throw new RuntimeException("wav文件太大:"+wavfile);
		if (wavFile.getNumChannels() > 1)
			throw new RuntimeException("wav文件不是单通道:"+wavfile);

		int[] data = new int[(int) wavFile.getNumFrames()];
		wavFile.readFrames(data, data.length);
		wavFile.close();
		return data;
	}

	public static String getWavInfo(File wavfile) throws Exception {
		WavFile wavFile = WavFile.openWavFile(wavfile);
		String info = wavFile.info();
		wavFile.close();
		return info;
	}

}
