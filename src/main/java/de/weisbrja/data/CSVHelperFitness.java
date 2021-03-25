package de.weisbrja.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;

public class CSVHelperFitness {

	private CSVPrinter csvPrinter;

	public CSVHelperFitness(String filename) {
		try {
			FileWriter fileWriter = new FileWriter(filename);
			csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180.withHeader("Generation", "Beste Fitness", "Mediane Fitness"));
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printValue(int generationCount, double bestFitness, double medianFitness) {
		try {
			csvPrinter.printRecord(generationCount, bestFitness, medianFitness);
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}