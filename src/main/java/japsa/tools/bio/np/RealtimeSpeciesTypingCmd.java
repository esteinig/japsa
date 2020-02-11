/*****************************************************************************
 * Copyright (c) Minh Duc Cao, Monash Uni & UQ, All rights reserved.         *
 *                                                                           *
 * Redistribution and use in source and binary forms, with or without        *
 * modification, are permitted provided that the following conditions        *
 * are met:                                                                  * 
 *                                                                           *
 * 1. Redistributions of source code must retain the above copyright notice, *
 *    this list of conditions and the following disclaimer.                  *
 * 2. Redistributions in binary form must reproduce the above copyright      *
 *    notice, this list of conditions and the following disclaimer in the    *
 *    documentation and/or other materials provided with the distribution.   *
 * 3. Neither the names of the institutions nor the names of the contributors*
 *    may be used to endorse or promote products derived from this software  *
 *    without specific prior written permission.                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
 ****************************************************************************/

/*****************************************************************************
 *                           Revision History                                
 * 7 Aug 2015 - Minh Duc Cao: Created                                        
 * 
 ****************************************************************************/
package japsa.tools.bio.np;

import java.io.IOException;

import japsa.bio.np.RealtimeSpeciesTyping;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;

/**
 * @author minhduc
 *
 */
@Deployable(
	scriptName = "jsa.np.rtSpeciesTyping", 
	scriptDesc = "Realtime species typing using Nanopore Sequencing data",
	seeAlso = "jsa.np.npreader, jsa.np.rtStrainTyping, jsa.np.rtResistGenes, jsa.util.streamServer, jsa.util.streamClient"
	)
public class RealtimeSpeciesTypingCmd extends CommandLine {

	public RealtimeSpeciesTypingCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		addString("output", "output.dat",  "Output file, - for standard output");		
		addString("bamFile", null,  "The bam file",true);		
		addString("indexFile", null,  "indexFile ",true);
		
		addDouble("qual", 1,  "Minimum alignment quality");
		addBoolean("twodonly", false,  "Use only two dimentional reads");
		
		addDouble("alpha", 0.05, "Paramater alpha from multinomialCI");
		addInt("minCount", 0, "Mininum number of mapped reads for a species to be considered");
		addString("filter", "", "List of species (separated by semicolon) to excluded from typing");

		addInt("read", 50,  "Minimum number of reads between analyses");		
		addInt("time", 30,   "Minimum number of seconds between analyses");

		addBoolean("web", false, "Whether to use Web visualization.");
		addBoolean("log", false, "Whether to write mapping details to species2reads.map.");
		addStdHelp();		
	} 
	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		CommandLine cmdLine = new RealtimeSpeciesTypingCmd();		
		args = cmdLine.stdParseLine(args);		

		/**********************************************************************/

		String output    = cmdLine.getStringVal("output");
		String bamFile   = cmdLine.getStringVal("bamFile");			
		String indexFile = cmdLine.getStringVal("indexFile");
		String filter = cmdLine.getStringVal("filter");

		int number       = cmdLine.getIntVal("read");
		int time       = cmdLine.getIntVal("time");		
		double qual      = cmdLine.getDoubleVal("qual");				
		boolean twoOnly      = cmdLine.getBooleanVal("twodonly");
		RealtimeSpeciesTyping.JSON = cmdLine.getBooleanVal("web");
		RealtimeSpeciesTyping.OUTSEQ = cmdLine.getBooleanVal("log");
		RealtimeSpeciesTyping.ALPHA = cmdLine.getDoubleVal("alpha");
		RealtimeSpeciesTyping.MIN_READS_COUNT = cmdLine.getIntVal("minCount");
		
		RealtimeSpeciesTyping paTyping = new RealtimeSpeciesTyping(indexFile, output);
		paTyping.setMinQual(qual);
		paTyping.setTwoOnly(twoOnly);	
		paTyping.setFilter(filter);
		paTyping.typing(bamFile, number, time);		
	}
}

/*RST*
----------------------------------------------------------------------------------
*jsa.np.rtSpeciesTyping*: Bacterial species typing with Oxford Nanopore sequencing
----------------------------------------------------------------------------------

*jsa.np.rtSpeciesTyping* identify proportions of species from a DNA sample 
using Oxford Nanopore sequencing in real-time. It reads data in SAM/BAM format
of the alignments of sequence reads to a collection of species genomes.

We provide a genome collection of nearly 1500 bacterial species
on  http://data.genomicsresearch.org/Projects/npAnalysis/.
Refer to the documentation at https://github.com/mdcao/npAnalysis/ for more 
details.
 
<usage> 

~~~~~~~~~~~~~~
Usage examples
~~~~~~~~~~~~~~

If there is a sam/bam file of aligning the Nanopore sequencing to the genome 
collection, the program can read from this
::

   jsa.np.rtSpeciesTyping -bam alignment.sam -index SpeciesTyping/Bacteria/speciesIndex --read 50 -time 60 -out speciesTypingResults.out
   
   
This program can read data from the output stream of an alignment program to
perform analysis in real-time. For example, one can create such a pipeline
to listen on port 3456
::

  jsa.util.streamServer -port 3456 \
  | bwa mem -t 10 -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -Y -K 10000 SpeciesTyping/Bacteria/genomeDB.fasta - 2> /dev/null \
  | jsa.np.rtSpeciesTyping -bam - -index SpeciesTyping/Bacteria/speciesIndex --read 50 -time 60 -out speciesTypingResults.out 2>  speciesTypingResults.log &
  
  
and streams data to this pipeline using npReader:
::

  jsa.np.npreader -GUI -realtime -folder <DownloadFolder> -fail -output data.fastq -stream serverAddress:3456


*RST*/
