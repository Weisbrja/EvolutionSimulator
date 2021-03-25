package de.weisbrja.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;

public class CSVHelperSpecies {

	private CSVPrinter csvPrinter;

	public CSVHelperSpecies(String filename) {
		try {
			FileWriter fileWriter = new FileWriter(filename);
			csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180.withHeader("Generation", "Spezies", "Anzahl"));
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printValues(int generationCount, String species, int speciesCount) {
		species = species.replace("-", "--");
		try {
			csvPrinter.printRecord(generationCount, species, speciesCount);
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}