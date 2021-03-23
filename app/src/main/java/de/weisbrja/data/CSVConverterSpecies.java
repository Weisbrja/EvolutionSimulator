package de.weisbrja.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CSVConverterSpecies {

	public static void main(String[] args) {
		String filename = "species_79415794618.csv";
		String convertedFilename = "converted_species_79415794618.csv";

		CSVConverterSpecies csvConverterSpecies = new CSVConverterSpecies();
		try {
			csvConverterSpecies.convert(filename, convertedFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void convert(String filename, String convertedFilename) throws IOException {
		// initialize the csv parser and get the record list
		FileReader fileReader = new FileReader(filename);
		CSVParser csvParser = CSVParser.parse(fileReader, CSVFormat.RFC4180.withFirstRecordAsHeader());
		List<CSVRecord> csvRecordList = csvParser.getRecords();

		Set<String> speciesSet = new HashSet<>();
		for (CSVRecord csvRecord : csvRecordList)
			speciesSet.add(csvRecord.get("Spezies"));

		StringBuilder header = new StringBuilder("Generation");
		for (String species : speciesSet)
			header.append(',').append(species);
		header.append('\n');

		// initialize the csv printer and write the header to the file
		FileWriter fileWriter = new FileWriter(convertedFilename);
		fileWriter.write(header.toString());
		fileWriter.flush();
		CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180.withFirstRecordAsHeader());

		// write the data to the file
		int currentGenerationCount = 0;
		Map<String, Integer> speciesDistributionInGeneration = new HashMap<>();

		for (CSVRecord csvRecord : csvRecordList) {
			int generationCount = Integer.parseInt(csvRecord.get("Generation"));
			String species = csvRecord.get("Spezies");
			int speciesCount = Integer.parseInt(csvRecord.get("Anzahl"));

			if (generationCount != currentGenerationCount) {
				ArrayList<Integer> record = new ArrayList<>();
				record.add(currentGenerationCount);
				for (String headerSpecies : speciesSet)
					record.add(speciesDistributionInGeneration.getOrDefault(headerSpecies, 0));
				csvPrinter.printRecord(record);
				csvPrinter.flush();
				currentGenerationCount = generationCount;
				speciesDistributionInGeneration.clear();
			}

			speciesDistributionInGeneration.put(species, speciesCount);
		}
	}
}