/* 
*    Ref-Finder
*    Copyright (C) <2015>  <PLSE_UCLA>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
//*
package metapackage;

import java.io.File;

public class MetaInfo {
	public static final int k=1; 
	public static final int minConcFact=3;
	public static final int beamSize=100;
	public static final double accuracy =0.75;
	public static final int maxException=10;

	public static final String baseDir = lsclipse.LSclipse.getDefault().getStateLocation().toOSString();
//	public static String srcDir = baseDir+"\\input";
//	public static String resDir = baseDir+"\\output";
//	public static String fdbDir = baseDir+"\\fdb";
	public static final File srcDir = new File(baseDir,"input");
	public static final File resDir = new File(baseDir,"output");
	public static final File fdbDir = new File(baseDir,"fdb");
	public static final File lsclipseDir = new File(srcDir, "lsclipse");
	public static final File uccDir = new File(baseDir, "ucc");
	
	public static final File included2kb = new File (srcDir, "2KB_lsdPred.rub");
	public static final File includedDelta = new File (srcDir, "deltaKB_lsdPred.rub");
//	public static String inFile2K=  new File (srcDir, version + "2KB.rub").getAbsolutePath();
//	public static String deltaFile = new File(srcDir, version + "delta.rub").getAbsolutePath();
	public static final String winnowings = new File(srcDir, "winnowingRules.rub").getAbsolutePath();
	public static final String modifiedWinnowings = new File(srcDir, "convertedwinnowingRules.rub").getAbsolutePath();

	public static final String lsclipse2KB = new File(lsclipseDir, "2KB_lsclipsePred.rub").getAbsolutePath();
	public static final String lsclipseDelta = new File(lsclipseDir, "deltaKB_lsclipsePred.rub").getAbsolutePath();

	public static final String lsclipseRefactorPred = new File(lsclipseDir, "preds1.rub").getAbsolutePath();
	public static final String lsclipseRefactorDeltaPrimed = new File(lsclipseDir, "deltaKB_primed_lsdPred.rub").getAbsolutePath();

	public static final String resultsFile = new File(resDir, "Hierarchical_lsclipse_Temp.rub").getAbsolutePath();
	public static final String ongoingResultsFile = new File(resDir, "exmp.rub").getAbsolutePath();
	public static final String refsOnlyFile = new File(resDir, "output.rub").getAbsolutePath();
	public static final String exportLineFile = new File(resDir, "lines.csv").getAbsolutePath();
	public static final String uccCountFile = new File(uccDir, "outfile_refactor_results.csv").getAbsolutePath();
/*
	public static String get2KB(int i){
		File projectDir = new File (srcDir, folderName);
		if (!projectDir.exists()) projectDir.mkdir();
		inFile2K = new File (projectDir, version[i] + "2KB.rub").getAbsolutePath();
		return inFile2K;
	}
	
	public static String getDelta(int i){
		File projectDir = new File (srcDir, folderName);
		if (!projectDir.exists()) projectDir.mkdir();
		deltaFile = new File (projectDir, version[i] + "delta.rub").getAbsolutePath();
		return deltaFile;
	}
*/
	
}
//*/