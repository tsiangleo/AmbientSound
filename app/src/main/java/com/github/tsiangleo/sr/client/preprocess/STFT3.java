package com.github.tsiangleo.sr.client.preprocess;
/* Copyright 2014 Eddy Xiao <bewantbe@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;


// Short Time Fourier Transform
public class STFT3 {
	
	private double[] spectrumAmpOutTmp;
	private double[] spectrumAmpOut;
	private double[] spectrumAmpOutDB;
	private double[] spectrumAmpIn;
	private double[] spectrumAmpInTmp;
	private double[] wnd;
	
	private int fftLen;
	private int hopSize;
	private int totalFrame;
	
	private int spectrumAmpPt;
	private double[][] spectrumAmpOutArray;
	private int spectrumAmpOutArrayPt = 0; // Pointer for spectrumAmpOutArray
	private RealDoubleFFT spectrumAmpFFT;

	private double sqr(double x) {
		return x * x;
	}

	private void initWindowFunction(int fftlen, String wndName) {
		wnd = new double[fftlen];
		if (wndName.equals("Bartlett")) {
			for (int i = 0; i < wnd.length; i++) { // Bartlett
				wnd[i] = Math.asin(Math.sin(Math.PI * i / wnd.length))
						/ Math.PI * 2;
			}
		} else if (wndName.equals("Hanning")) {
			for (int i = 0; i < wnd.length; i++) { // Hanning, hw=1
			// wnd[i] = 0.5*( 1-Math.cos(2*Math.PI*i/(wnd.length-1.)));
				wnd[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i
						/ (wnd.length - 1.))) * 2;
			}
		} else if (wndName.equals("Blackman")) {
			for (int i = 0; i < wnd.length; i++) { // Blackman, hw=2
				wnd[i] = 0.42 - 0.5
						* Math.cos(2 * Math.PI * i / (wnd.length - 1)) + 0.08
						* Math.cos(4 * Math.PI * i / (wnd.length - 1));
			}
		} else if (wndName.equals("Blackman Harris")) {
			for (int i = 0; i < wnd.length; i++) { // Blackman_Harris, hw=3
				wnd[i] = (0.35875 - 0.48829
						* Math.cos(2 * Math.PI * i / (wnd.length - 1))
						+ 0.14128
						* Math.cos(4 * Math.PI * i / (wnd.length - 1)) - 0.01168 * Math
						.cos(6 * Math.PI * i / (wnd.length - 1))) * 2;
			}
		}
		else {
			for (int i = 0; i < wnd.length; i++) {
				wnd[i] = 1;
			}
		}
	}

	
	private void init(int fftlen, int hopsize, int sampleLength) {
		if (hopsize <= 0) {
			throw new IllegalArgumentException(
					"STFT::init(): should hopSize > 0.");
		}
		if (((-fftlen) & fftlen) != fftlen) {
			// error: fftlen should be power of 2
			throw new IllegalArgumentException(
					"STFT::init(): Currently, only power of 2 are supported in fftlen");
		}
		
		fftLen = fftlen;
		hopSize = hopsize;
		
		spectrumAmpOutTmp = new double[fftlen / 2 + 1];
		spectrumAmpOut = new double[fftlen / 2 + 1];
		spectrumAmpOutDB = new double[fftlen / 2 + 1];
		spectrumAmpIn = new double[fftlen];
		spectrumAmpInTmp = new double[fftlen];
		spectrumAmpFFT = new RealDoubleFFT(fftlen);
		
		/**
		 * 在这里 帧长=傅里叶点数=fftlen。 帧移=帧长的一半。
		 * 
		 * math.ceil(x)返回大于参数x的最小整数,即对浮点数向上取整 FIXME sampleLength应该是该输入序列的长度
		 */

		// 按照feedData的方式的话，总的帧的数目如下。
		int numFrame = (int) ((sampleLength - fftlen) / (float)hopsize + 1);
		totalFrame = (int) Math.ceil(numFrame);
		
		System.out.println("totalFrame:" + totalFrame);

		// spectrumAmpOutArray = new double[(int)Math.ceil((double)minFeedSize /
		// (fftlen/2))][]; // /2 since half overlap
		spectrumAmpOutArray = new double[totalFrame][]; // /2 since half overlap

		for (int i = 0; i < spectrumAmpOutArray.length; i++) {
			spectrumAmpOutArray[i] = new double[fftlen / 2 + 1];
		}

		initWindowFunction(fftlen, "Hanning");
	}

	public STFT3(int fftlen, int hopsize, int sampleLength) {
		init(fftlen, hopsize, sampleLength);
	}

	public double[][] feedData(int[] ds) {
		return feedData(ds, ds.length);
	}
	/**
	 * 
	 * @param ds
	 * @param dsLen
	 * @return
	 */
	public double[][] feedData(int[] ds, int dsLen) {
		if (dsLen > ds.length) {
			dsLen = ds.length;
		}
		int inLen = spectrumAmpIn.length;
		int outLen = spectrumAmpOut.length;
		int dsPt = 0; // input data point to be read
		while (dsPt < dsLen) {
			// 将ds数组复制到spectrumAmpIn数组
			while (spectrumAmpPt < inLen && dsPt < dsLen) {
				double s = ds[dsPt++];
				spectrumAmpIn[spectrumAmpPt++] = s;
			}
			if (spectrumAmpPt == inLen) { // enough data for one FFT
				// 乘以窗函数。
				for (int i = 0; i < inLen; i++) {
					spectrumAmpInTmp[i] = spectrumAmpIn[i] * wnd[i];
				}
				//快速傅里叶变换
				spectrumAmpFFT.ft(spectrumAmpInTmp);
				
				//log
				fftToAmp2(spectrumAmpOutTmp, spectrumAmpInTmp);
				
				//复制到spectrumAmpOutArray
				System.arraycopy(spectrumAmpOutTmp, 0,
						spectrumAmpOutArray[spectrumAmpOutArrayPt], 0,
						spectrumAmpOutTmp.length);
				spectrumAmpOutArrayPt = (spectrumAmpOutArrayPt + 1)
						% spectrumAmpOutArray.length;
				
				// half overlap (set spectrumAmpPt = 0 for no overlap)
//				int n2 = spectrumAmpIn.length / 2;
//				System.arraycopy(spectrumAmpIn, n2, spectrumAmpIn, 0, n2);
//				spectrumAmpPt = n2;
				
				//帧移
				System.arraycopy(spectrumAmpIn, hopSize, spectrumAmpIn, 0, spectrumAmpIn.length - hopSize);
				spectrumAmpPt = spectrumAmpIn.length - hopSize;
			}
		}

		return spectrumAmpOutArray;
	}

	private void fftToAmp2(double[] dataOut, double[] data) {
		dataOut[0] = Math.abs(Math.sqrt((data[0] * data[0])));
		dataOut[0] = 20 * Math.log10(dataOut[0] / 10e-6);
		int j = 1;
		for (int i = 1; i < data.length - 1; i += 2, j++) {
			dataOut[j] = Math.abs(Math.sqrt((data[i] * data[i] + data[i + 1]
					* data[i + 1])));
			dataOut[j] = 20 * Math.log10(dataOut[j] / 10e-6);
		}
		dataOut[j] = Math.abs(Math
				.sqrt((data[data.length - 1] * data[data.length - 1])));
		dataOut[j] = 20 * Math.log10(dataOut[j] / 10e-6);

	}

	public void clear() {
		spectrumAmpPt = 0;
		Arrays.fill(spectrumAmpOut, 0.0);
		Arrays.fill(spectrumAmpOutDB, Math.log10(0));
		for (int i = 0; i < spectrumAmpOutArray.length; i++) {
			Arrays.fill(spectrumAmpOutArray[i], 0.0);
		}
	}

	public static void main(String[] args) {
//		STFT3 stft = new STFT3(16, 16000, "Hanning");
//		System.out.println(Arrays.toString(stft.wnd));

		// int wndlength = 16;
		// double[] wnd = new double[wndlength];
		// for (int i=0; i<wndlength; i++) { // Hanning, hw=1
		// wnd[i] = 0.5*( 1-Math.cos(2*Math.PI*i/(wndlength-1.)));
		// // wnd[i] = 0.5*( 1-Math.cos(2*Math.PI*i/(wnd.length-1.))) *2;
		// }
		// System.out.println(Arrays.toString(wnd));
	}
}