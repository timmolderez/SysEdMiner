package ca.pfv.spmf.tools.dataset_generator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to add timestamps automatically to a sequence database
 * in SPMF format
 */
public class MainTestAddTimeStampsToSequenceDatabase {
	
	public static void main(String [] arg) throws IOException{
		
		String inputFile = fileToPath("contextPrefixspan.txt");
		String outputFile = ".//output.txt";
		
		AddTimeStampsToSequenceDatabase converter = new AddTimeStampsToSequenceDatabase();
		converter.convert(inputFile, outputFile);
	}

	

	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestAddTimeStampsToSequenceDatabase.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
