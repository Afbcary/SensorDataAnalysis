import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

public class mainAnalyzer {

	public static void main(String[] args) {
		// load the data from the file
		File rightTurnDataFile = new File("RightTurnData.csv");

		Scanner rT;
		try {
			System.out.println();
			System.out.println("Right turn data");
			System.out.println("-----------------------");
			rT = new Scanner(rightTurnDataFile);
			List<double[]> rTPoints = getDataPoints(rT);

			// test that the data was correctly imported
			// myDataTest(rTPoints);

			rTPoints = eWMAverage(rTPoints);

			// smooth the data
			// first I'll try to interpret the data without any smoothing

			// calibrate the data

			// compute the distances traveled and the turn angles
			// TODO
			// ACCELEROMETER X (m/s²),ACCELEROMETER Y (m/s²),ACCELEROMETER Z
			// (m/s²),GYROSCOPE X (rad/s),GYROSCOPE Y (rad/s),GYROSCOPE Z
			// (rad/s),Time (ms)

			double[] finalDistancesAndVelocities = aToSandV(rTPoints);
			System.out.println();
			System.out
					.println("The distances and velocities need more work in"
							+ " corrections and noise elimination to be more accurate.");

			double[][] turnDegrees = getTurnDegrees(rTPoints);
			System.out.println();
			System.out
					.println("The one turn in the right turn data was represented by four"
							+ " different positive and negative peaks in the gyroscope data. Only one was significant.");
			System.out
					.println("The 90 degree turn right was near enough to 90 degrees and importantly in the negative direction.");
			System.out.println();
			// close scanner
			rT.close();
		} catch (FileNotFoundException e) {

			System.out.println("right turn file not found");
			e.printStackTrace();
		}

		// Load the data from the file
		File multipleTurnDataFile = new File("MultipleTurnData.csv");

		Scanner mT;
		try {
			System.out.println();
			System.out.println("Multiple turn data");
			System.out.println("-----------------------");
			mT = new Scanner(multipleTurnDataFile);
			List<double[]> mTPoints = getDataPoints(mT);
			// TODO
			mTPoints = eWMAverage(mTPoints);
			aToSandV(mTPoints);
			getTurnDegrees(mTPoints);
			System.out.println();

			System.out
					.println("I first disregard the insignificant 'turns' the program picks up.");
			System.out
					.println("The same calculations yield a 90 degree right turn, 90 degree left turn, 40 degree left turn, and 90 degree left turn.");

			// close scanner
			mT.close();
		} catch (FileNotFoundException e) {
			System.out.println("multiple turn file not found");
			e.printStackTrace();
		}

	}

	private static double[][] getTurnDegrees(List<double[]> points) {
		// each time the gyro records greater than +-0.05 rad/s, create a
		// separate turn with the duration and the average rads/s.
		// I determined that only the gyro in the Z direction records meaningful
		// data in the RightTurnData file. A negative value indicates a
		// right turn and a positive value indicates a left turn.
		double[] cRow = points.get(0);

		// create a list of arrays to store the data for each "turn" the program
		// detects
		// assuming there are less than 100 turns in a file.
		int maxNumTurns = 100;
		// each turn has an total rad/s number and a duration in sec associated
		// with it.
		double[][] turns = new double[maxNumTurns][2];
		double totalRad = 0;
		double totalDuration = 0;
		int currentTurn = 0;
		double cRad = 0;
		double numRad = 0;
		double pTime = 0;

		for (int r = 0; cRow.length == 7; r++) {
			cRad = cRow[5];
			if ((cRad > 0.05) || (cRad < -0.05)) {
				totalRad += cRad;
				totalDuration += cRow[6] - pTime;
				numRad++;

			} else if (totalDuration > 0) {
				// end the current turn
				// convert the duration to seconds
				totalDuration = totalDuration / 1000;
				turns[currentTurn][1] = totalDuration;
				turns[currentTurn][0] = totalRad / numRad;
				// increment turn number
				currentTurn++;
				// reset turn measurements
				totalDuration = 0;
				totalRad = 0;
				numRad = 0;

			}
			if (points.size() > (r + 1)) {
				pTime = cRow[6];
				cRow = points.get(r + 1);
			} else {
				break;
			}
		}

		// print out the turns
		double tDegs = 0;
		for (int s = 0; s < maxNumTurns && turns[s][1] > 0; s++) {
			System.out.println();
			System.out.println("Turn " + (s + 1) + " had totalDur: "
					+ turns[s][1] + " and an average rad/s: " + turns[s][0]);
			// multiply by the rads to degrees conversion factor.
			tDegs = (turns[s][0] * turns[s][1] * 57.2958);
			System.out.println("So the total degrees in this turn is: " + tDegs
					+ ".");

		}
		if (turns[0][1] == 0) {
			System.out.println("no turns??!");
		}
		return turns;
	}

	private static List<double[]> eWMAverage(List<double[]> rTPoints) {
		// Exponentially weighted moving average for the acceleration and gyro
		// measurements

		List<double[]> eWMAPoints = rTPoints;
		double[] pRow;
		double[] cRow = eWMAPoints.get(1);
		// weight
		double w = 0.2;
		for (int r = 0; cRow.length == 7; r++) {

			pRow = eWMAPoints.get(r);

			cRow[0] = w * cRow[0] + (1 - w) * pRow[0];
			cRow[1] = w * cRow[1] + (1 - w) * pRow[1];
			cRow[2] = w * cRow[2] + (1 - w) * pRow[2];
			cRow[3] = w * cRow[3] + (1 - w) * pRow[3];
			cRow[4] = w * cRow[4] + (1 - w) * pRow[4];
			cRow[5] = w * cRow[5] + (1 - w) * pRow[5];
			// current row updates at this time so that the condition is false
			// when there are no more data points
			if (r + 2 < eWMAPoints.size()) {
				cRow = eWMAPoints.get(r + 2);
			} else {
				break;
			}

		}
		return eWMAPoints;
	}

	// ACCELEROMETER X (m/s²),ACCELEROMETER Y (m/s²),ACCELEROMETER Z
	// (m/s²),GYROSCOPE X (rad/s),GYROSCOPE Y (rad/s),GYROSCOPE Z
	// (rad/s),Time (ms)
	private static double[] aToSandV(List<double[]> points) {

		double sX = 0;
		double sY = 0;
		double sZ = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double aX = 0;
		double aY = 0;
		// should aZ start as -9.8???
		double aZ = 0;
		// The previous time is needed to determine how much time the device has
		// spent accelerating in a certain direction.
		double pT = 0;
		double time = 0;
		// time interval
		double tI = 0;
		double[] row;
		// I'll try adding a baseline as even when the deviec isn't moving it
		// records values for acceleration that aren't 0, 0, -9.8 as is expected
		// in a perfect world

		// lets calculate the baseline using a number of data points from when
		// the cart was at rest (40) and taking the average
		double xBase = points.get(0)[0];
		double yBase = points.get(0)[1];
		double zBase = points.get(0)[2];

		for (int r = 1; r < 40; r++) {
			row = points.get(r);
			xBase += row[0];
			yBase += row[1];
			zBase += row[2];
		}
		xBase = xBase / 40;
		yBase = yBase / 40;
		zBase = zBase / 40;

		for (int d = 0; d < points.size(); d++) {
			row = points.get(d);
			// to avoid index out of bounds exception when there isn't any more
			// data
			if (row.length == 7) {
				time = row[6];
			} else {
				break;
			}
			aX = row[0];
			aY = row[1];
			aZ = row[2];

			// Apply baseline correction
			aX = aX - xBase;
			aY = aY - yBase;
			aZ = aZ - zBase;

			tI = time - pT;

			sX = sX + (vX * tI) + (0.5 * aX * Math.pow(tI, 2));
			sY = sY + (vY * tI) + (0.5 * aY * Math.pow(tI, 2));
			sZ = sZ + (vZ * tI) + (0.5 * aZ * Math.pow(tI, 2));

			// TODO is this right?
			// displacements calculated first so they use the previous speeds

			vX = vX + aX * tI;
			vY = vY + aY * tI;
			vZ = vZ + aZ * tI;
			pT = time;
		}

		// lets test what sort of distance values this produces in its
		// rudimentary form.
		System.out.println("distance x = " + sX);
		System.out.println("distance y = " + sY);
		System.out.println("distance z = " + sZ);
		// I'll start printing the velocity values too for my own records
		System.out.println();
		System.out.println("velocity x = " + vX);
		System.out.println("velocity y = " + vY);
		System.out.println("velocity z = " + vZ);

		double[] sAndV = { sX, sY, sZ, vX, vY, vZ };
		return sAndV;

	}

	private static List<double[]> getDataPoints(Scanner scnr) {
		List<double[]> points = new ArrayList<double[]>();

		// skip the first line of the file with the column headers
		scnr.nextLine();
		while (scnr.hasNextLine()) {
			String currLine = scnr.nextLine();
			// add a new line to the 2 dimensional arraylist and input the data
			String[] stringArray = currLine.split(",");
			double[] doubleArray = new double[stringArray.length];
			for (int i = 0; i < stringArray.length; i++) {
				String numberAsString = stringArray[i];
				doubleArray[i] = Double.parseDouble(numberAsString);
			}
			points.add(doubleArray);

		}

		return points;
	}

	private static void myDataTest(List<double[]> points) {

		int totalPoints = points.size();

		// test if the first line is properly skipped (line 2 of file)
		printLine(points.get(0));
		// test an intermediate line : for example line 21
		printLine(points.get(19));

		// print out how many lines points has
		System.out.println();
		System.out.println("this file has " + totalPoints + " lines.");

		// the last line is blank. How many lines aren't being read in?
		for (int j = 5500; j < totalPoints; j = j + 10) {
			System.out.println();
			System.out.println("~" + j + "th point equals");
			printLine(points.get(j));
		}
		// test if the last line is read in
		System.out
				.println("slightly confused why this follwing test is failing but overall the program seems to work thusfar.");
		printLine(points.get(5591));

	}

	private static void printLine(double[] line) {
		System.out.println();
		for (int i = 0; i < line.length; i++) {
			System.out.print(line[i] + " , ");
		}

	}

}
