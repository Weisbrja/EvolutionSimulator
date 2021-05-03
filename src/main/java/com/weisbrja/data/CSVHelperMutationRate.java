package com.weisbrja.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;

public class CSVHelperMutationRate {

	private CSVPrinter csvPrinter;

	public CSVHelperMutationRate(String filename) {
		try {
			FileWriter fileWriter = new FileWriter(filename);
			csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180.withHeader("Generation", "Mediane Mutationsrate", "Mediane strukturelle Mutationsrate"));
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printValue(int generationCount, double medianMutationRate, double medianStructuralMutationRate) {
		try {
			csvPrinter.printRecord(generationCount, medianMutationRate, medianStructuralMutationRate);
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}