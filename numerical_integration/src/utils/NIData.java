package utils;

import hadoop.FloatIntervalInputFormat;
import hadoop.FloatIntervalOutputFormat;
import integration.FloatInterval;
import integration.IInterval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;

public class NIData {

	private static final Class<NIData> CLAZZ = NIData.class;

	public static final String WHITESPACE = " ";
	public static final String CHARSET = "UTF-8";

	public enum Argument {
		OUTPUT("output", 0), START("start", 1), END("end", 2), INTERVALS(
				"intervals", 3), RESOLUTION("resolutionPerInterval", 4), FS(
				Argument.DFS + "|" + Argument.LFS, 5);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public static final String DFS = "dfs";
		public static final String LFS = "lfs";
	}

	public static void main(String[] args) {
		if (args.length < 6) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(1);
		}

		final String output = args[Argument.OUTPUT.index];
		final float start = Float.parseFloat(args[Argument.START.index]);
		final float end = Float.parseFloat(args[Argument.END.index]);
		final int count = Integer.parseInt(args[Argument.INTERVALS.index]);
		final int resolution = Integer
				.parseInt(args[Argument.RESOLUTION.index]);
		final String fs = args[Argument.FS.index];

		List<IInterval<Float>> intervals = generateIntervals(start, end, count,
				resolution);

		if (Argument.DFS.equals(fs))
			writeToDFS(intervals, output, WHITESPACE);
		else if (Argument.LFS.equals(fs))
			writeToLFS(intervals, output, WHITESPACE);
		else
			Logger.logError(CLAZZ, "Unknown file system!");
	}

	public static List<IInterval<Float>> generateIntervals(float start,
			float end, int count, int resolution) {
		List<IInterval<Float>> intervals = new ArrayList<IInterval<Float>>(
				count);
		final float offset = (end - start) / count;
		IInterval<Float> interval = null;

		float tmpStart;
		for (int i = 0; i < count; i++) {
			tmpStart = start + i * offset;
			interval = new FloatInterval(tmpStart, tmpStart + offset,
					resolution);
			intervals.add(interval);
		}

		return intervals;
	}

	public static boolean writeToLFS(List<IInterval<Float>> intervals,
			String file) {
		return writeToLFS(intervals, file, WHITESPACE);
	}

	public static boolean writeToLFS(List<IInterval<Float>> intervals,
			String file, final String whitespace) {
		boolean res = true;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			write(fos, intervals, whitespace);
		} catch (IOException e) {
			Logger.logError(CLAZZ, "Could not write input data.");
			e.printStackTrace();
			res = false;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return res;
	}

	public static boolean writeToDFS(List<IInterval<Float>> intervals,
			String file) {
		return writeToDFS(intervals, file, WHITESPACE);
	}

	public static boolean writeToDFS(List<IInterval<Float>> intervals,
			String file, final String whitespace) {
		boolean res = true;

		try {
			FsUrlStreamHandlerFactory factory = new org.apache.hadoop.fs.FsUrlStreamHandlerFactory();
			java.net.URL.setURLStreamHandlerFactory(factory);
		} catch (Exception e) {
			Logger.logWarn(
					CLAZZ,
					"Could not set org.apache.hadoop.fs.FsUrlStreamHandlerFactory. May be it has been set before.");
		}

		Configuration configuration = new Configuration(true);

		FileSystem fs = null;
		FSDataOutputStream fos = null;
		try {
			fs = FileSystem.get(configuration);

			fos = fs.create(new Path(file));
			write(fos, intervals, whitespace);
		} catch (IOException e) {
			Logger.logError(CLAZZ, "Could not write input data.");
			e.printStackTrace();
			res = false;
		} finally {
			if (fs != null)
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return res;
	}

	private static void write(OutputStream os,
			List<IInterval<Float>> intervals, final String whitespace)
			throws UnsupportedEncodingException, IOException {
		for (IInterval<Float> i : intervals) {
			os.write(FloatIntervalOutputFormat.createString(i, whitespace)
					.getBytes(CHARSET));
			os.write("\n".getBytes(CHARSET));
		}
	}

	public static List<IInterval<Float>> readIIntervals(File file) {
		return readIIntervals(file, WHITESPACE);
	}

	public static List<IInterval<Float>> readIIntervals(File file,
			final String separator) {
		List<IInterval<Float>> intervals = new LinkedList<IInterval<Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			IInterval<Float> interval;
			while (sc.hasNext()) {
				line = sc.nextLine();

				interval = FloatIntervalInputFormat
						.createFloatIntervalWritable(line);
				intervals.add(interval);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return intervals;
	}

}