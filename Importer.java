/**
Bionic Dues Character Importer
-------------------------------
This program takes characters from previous campaigns in Bionic Dues and imports them into a new game save.
It does this by looking at samples of save code, isolating the differences as item information, and applying this item information to a new save.
Further testing needs to be done on the following:
	Changing non-Epics to Epics.
	Changing Epics to non-Epics. (not sure why you'd do this, but hey, it's your save)
Further development needs to be done on the following:
	Testing assault and brawler exos.
	Testing multiples of exos.
	More detailed error messages when exceptions are thrown.
*/

import java.util.*;
import java.io.*;

public class Importer {
	static int numberOfSaves = 27; /** number of save file samples needed */
	
	public static void main(String args[]) {
		instructions();
		//read saves into array
		String[] rawSaves = getSaves();
		
		//isolate exo code
		String[][] parsedSaves = parseSaves(rawSaves);
		
		//trim last exo's entries so that non-item data is not included
		parsedSaves = filterNonItem(parsedSaves);
		
		//AT THIS POINT, EACH SAVE HAS BEEN REDUCED TO 4 EXOS PER SAVE, EACH ON ITS OWN LINE
		
		//compare element 27 with element 26, find the difference and location of difference; then compare 26 and 25 and do the same (this way, files can be editted backwards, indexing from the name as the starting position)//USE RECURSION
		//27 is the blank template of everything
		ArrayList<String> results = compareExos(parsedSaves);
		
		//remove invalid entries (ones that don't start with 1 after the second :)
		results = filterInvalid(results);
		
		//AT THIS POINT, ALL CHANGES THAT NEED TO BE MADE ARE STORED IN results IN THE FOLLOWING FORMAT:
		//		<exo name>:<location relative to the exo name with the name being 0>:<item data>
		
		//import New.save
		String newGame = getNewGame();
		
		//apply changes from results to newGame
		newGame = applyChanges(results, newGame);
		
		//overwrite content in New.save with newGame
		try {
			PrintWriter lineOutput = new PrintWriter("New.save");
			lineOutput.println(newGame);
			lineOutput.close();
		} catch(FileNotFoundException e) {
			System.out.println("New.save not found. Please ensure that New.save is in the same folder as this program then try again.");
			System.exit(1);
		}
	}
	
	public static String applyChanges(ArrayList<String> results, String newGame) {
		String alteredGame = newGame;
		for(int i = 0; i < results.size(); i++) {
			//break the change to be made into 3 parts: exo name, change location, change data
			String change = results.get(i);
			String exoName = change.substring(0, change.indexOf(":"));
			change = change.substring(change.indexOf(":") + 1);
			int changeLocation = Integer.valueOf(change.substring(0, change.indexOf(":")));
			String changeData = change.substring(change.indexOf(":") + 1);
			//break alteredGame into 2 parts: before exo, exo data
			String firstHalf = alteredGame.substring(0, alteredGame.indexOf(exoName));
			String secondHalf = alteredGame.substring(alteredGame.indexOf(exoName));
			//move pre-change data to firstHalf based on changeLocation
			for(int j = 0; j < changeLocation; j++) {
				firstHalf += secondHalf.substring(0, secondHalf.indexOf(",") + 1); //take trailing commas with the data
				secondHalf = secondHalf.substring(secondHalf.indexOf(",") + 1);
			}
			//remove the blank item slot
			secondHalf = secondHalf.substring(secondHalf.indexOf(",") + 1);
			//add change data to the beginning of secondHalf
			secondHalf = changeData + secondHalf;
			//merge halves back into alteredGame
			alteredGame = firstHalf + secondHalf;
		}
		return alteredGame;
	}
	
	public static String getNewGame() {
		String text = "";
		File file = new File("New.save");
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			text = br.readLine();
			br.close();
		} catch(IOException e) {
			System.out.println("Please ensure New.save is in the same directory as this program.");
			System.exit(1);
		}
		return text;
	}
	
	public static void instructions() {
		/** INCLUDE NON-EPIC EXO TEMPLATES */
		System.out.println("INSTRUCTIONS");
		System.out.println("---------------------------");
		System.out.println("1. Open the save file containing the exos you would like to export.");
		System.out.println("2. Save this game as \'Trial 0\'");
		System.out.println("3. Using the diagram below, remove the item in slot 1 for each exo.");
		System.out.println();
		System.out.println("Epic Sniper                           Epic Siege                            Epic Ninja");
		System.out.println("1  2  3   4  5  6   7  8  9           1  2  3   4  5  6   7  8  9           1  2  3   4  5  6   7  8  9");
		System.out.println("10    16  22    23  13                10    15  21    22  13                       13  19    20  10");
		System.out.println("11    17            14                11    16            14                	   14            11");
		System.out.println("12    18  24        15                12    17  23    24                    	   15  21        12");
		System.out.println("       19  20  21                            18  19  20                     	    16  17  18");
		System.out.println();
		System.out.println("Epic Science                          Epic Brawler                          Epic Assault");
		System.out.println("1  2  3   4  5  6   7  8  9           1  2  3   4  5  6   7  8  9           1  2  3   4  5  6   7  8  9");
		System.out.println("      13  20    21  10                10    18  23    24  16    13          10    19  24    25  16    13");
		System.out.println("	   14            11               11    19            17    14          11    20            17    14");
		System.out.println("	       22    23  12               12    20                  15          12    21  26        18    15");
		System.out.println("	    15  16  17  18  19                   21  22                                22  23");
		System.out.println();
		System.out.println("4. Save the game as \'Trial 1\'");
		System.out.println("5. Continue to remove items one by one, each time saving the file \'Trial #\' where # is the slot number you removed an item from.");
		System.out.println("6. Once you have removed all items, you should have 27 save files numbered from 0 to 26.");
		System.out.println("7. If you have less than 26, re-save your last save file as the missing numbers.");
		System.out.println("8. Create a new game that has the same exos as you would like to import.");
		System.out.println("9. Save the new game as \'New\'.");
		System.out.println("10. Copy all Trial saves and New into the same directory as this program.");
		System.out.print("11. When you are ready to continue, press ENTER...");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
	}
	
	public static String[][] filterNonItem(String[][] parsedSaves) {
		//read in the 26th and 0th saves as baseline data
		String firstSample = parsedSaves[numberOfSaves - 1][0]; //0 is the end of the file due to reverse storing done at the end of parseSaves()
		String secondSample = parsedSaves[0][0];
		//find the key to the beginning of the end of the save file
		String key = "";
		for(int pos = 0; pos < firstSample.length(); pos++) {
			//create a string of 50 characters from the 26th save
			key = firstSample.substring(pos, pos + 50);
			//find the end of the 4th exo's code by looking for the first group of elements that appear in both a fully equipped and a stripped-down exo
			if(secondSample.contains(key)) {
				key = firstSample.substring(pos + 1, pos + 51); //shifting the index to the right by 1 is needed to start with a number rather than a comma
				break;
			}
		}
		//trim the end off of all entries in unfiltered that contain the key
		for(int i = 0; i < numberOfSaves; i++) {
			for(int j = 0; j < 4; j++) {
				String temp = "";
				if(parsedSaves[i][j].contains(key)) {
					parsedSaves[i][j] = parsedSaves[i][j].substring(0, parsedSaves[i][j].indexOf(key));
				}
			}
		}
		return parsedSaves;
	}
	
	public static ArrayList<String> filterInvalid(ArrayList<String> unfiltered) {
		ArrayList<String> filtered = new ArrayList<String>();
		//only include items with a 1 after the second :
		for(int i = 0; i < unfiltered.size(); i++) {
			String temp = unfiltered.get(i);
			temp = temp.substring(temp.indexOf(":") + 1);
			temp = temp.substring(temp.indexOf(":") + 1);
			if(temp.substring(0,1).equals("1")) {
				filtered.add(unfiltered.get(i));
			}
		}
		return filtered;
	}
	
	public static ArrayList<String> compareExos(String[][] parsedSaves) {
		ArrayList<String> results = new ArrayList<String>();
		//for each exo, find the differences across all saves
		for(int i = 0; i < 4; i++) {
			//get name of exo
			String name = parsedSaves[numberOfSaves - 1][i].substring(0, parsedSaves[numberOfSaves - 1][i].indexOf(","));
			for(int j = numberOfSaves - 1; j > 0; j--) {
				//store present and next exo lines to compare
				String first = parsedSaves[j][i];
				String second = parsedSaves[j - 1][i];
				//parse the line into elements separated by commas
				ArrayList<String> firstList = separate(first);
				ArrayList<String> secondList = separate(second);
				//find changes between each version of the exo
				String temp = findChange(firstList, secondList);
				if(temp != null) {
					results.add(name + ":" + temp);
				}
			}
		}
		return results;
	}
	
	public static String findChange(ArrayList<String> firstList, ArrayList<String> secondList) {
		//compare arraylists to find first point of difference
		int max = (firstList.size() < secondList.size()) ? firstList.size() : secondList.size();
		int startingElement = -1;
		for(int i = 0; i < max; i++) {
			if(!firstList.get(i).equals(secondList.get(i))) {
				startingElement = i;
				break;
			}
		}
		//handle what happens if no difference is found
		if(startingElement == -1) {
			return null;
		}
		//compare arraylists to find last point of difference
		int endingElement = -1;
		for(int i = firstList.size() - 1, j = secondList.size() - 1; i >= 0 && j >= 0; i--, j--) {
			if(!firstList.get(i).equals(secondList.get(j))) {
				endingElement = j;
				break;
			}
		}
		//store differing elements from secondList as result
		String result = String.valueOf(startingElement) + ":";
		for(int i = startingElement; i <= endingElement; i++) {
			result += secondList.get(i) + ",";
		}
		return result;
	}
	
	public static ArrayList<String> separate(String unprocessed) {
		//read unprocessed into an arraylist
		ArrayList<String> textList = new ArrayList<String>();
		while(unprocessed.indexOf(",") >= 0) {
			textList.add(unprocessed.substring(0, unprocessed.indexOf(",")));
			unprocessed = unprocessed.substring(unprocessed.indexOf(",") + 1);
		}
		return textList;
	}
	
	public static String[][] parseSaves(String[] rawSaves) {
		//separate save files into 4 exos (Assault, Brawler, Ninja, Science, Siege, Sniper)
		String[][] parsedSaves = new String[numberOfSaves][4];
		for(int i = 0; i < numberOfSaves; i++) {
			for(int j = 0; j < 4; j++) {
				parsedSaves[i][j] = "";
			}
		}
		for(int i = 0; i < numberOfSaves; i++) {
			String[] exoNames = new String[6];
			exoNames[0] = "Assault";
			exoNames[1] = "Brawler";
			exoNames[2] = "Sniper";
			exoNames[3] = "Siege";
			exoNames[4] = "Ninja";
			exoNames[5] = "Science";
			//read current save into a working copy
			String workingSave = rawSaves[i];
			//find location of all 4 exo names
			int[] nameLocations = new int[4];
			int count = 0;
			for(int j = 0; j < 6; j++) {
				if(workingSave.indexOf(exoNames[j]) != -1) {
					nameLocations[count] = workingSave.indexOf(exoNames[j]);
					count++;
				}
			}
			//sort nameLocations from smallest to largest
			Arrays.sort(nameLocations);
			//store exos into parsedSaves starting with the end of the file, trim the file afterwards
			int counter = 3;
			for(int j = 0; j < 4; j++) {
				parsedSaves[i][j] = workingSave.substring(nameLocations[counter]);
				workingSave = workingSave.substring(0, nameLocations[counter]);
				counter--;
			}
		}
		return parsedSaves;
	}
	
	public static String[] getSaves() {
		//turn saves into an array of strings
		String[] rawSaves = new String[numberOfSaves];
		for(int i = 0; i < numberOfSaves; i++) {
			//find file names in accordance with instructions
			String fileName = "Trial " + i + ".save";
			File file = new File(fileName);
			//read file into rawSaves
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line = null;
				rawSaves[i] = br.readLine();
				br.close();
			} catch(IOException e) {
				System.out.println();
				System.out.println("Please ensure that Trial " + i + ".save is in the same folder as this program then try again.");
				System.exit(1);
			}
		}
		return rawSaves;
	}
}
