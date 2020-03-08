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

/*                           Revision History                                
 * 28/05/2014 - Minh Duc Cao: Created                                        
 ****************************************************************************/

package japsadev.bio.hts;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import japsa.util.CommandLine;
import japsadev.util.HTSUtilities;
import japsa.util.deploy.Deployable;
import japsadev.util.HTSUtilities.IdentityProfile1;


/**
 * @author minhduc
 *
 */

@Deployable(
	scriptName = "jsa.hts.errorAnalysis",
	scriptDesc = "Error analysis of sequencing data")
public class HTSErrorAnalysisCmd2 extends CommandLine{
//	private static final Logger LOG = LoggerFactory.getLogger(HTSErrorAnalysisCmd.class);

	public HTSErrorAnalysisCmd2(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		addString("bamFile", null,  "Name of bam file", true);
		addString("reference", null, "Name of reference genome",true);
		addString("pattern", null, "Pattern of read name, used for filtering");
		addInt("qual", 0, "Minimum quality required");

		addStdHelp();		
	} 



	public static void main(String [] args) throws IOException, InterruptedException{		 		
		CommandLine cmdLine = new HTSErrorAnalysisCmd2();		
		args = cmdLine.stdParseLine(args);		

		String reference = cmdLine.getStringVal("reference");		
		int qual = cmdLine.getIntVal("qual");
		String pattern = cmdLine.getStringVal("pattern");
		String bamFile = cmdLine.getStringVal("bamFile");

		errorAnalysis(bamFile, reference, pattern, qual);		


		//paramEst(bamFile, reference, qual);
	}

	 

	/**
	 * Error analysis of a bam file. Assume it has been sorted
	 */
	static void errorAnalysis(String bamFileDir, String refFile, String pattern, int qual) throws IOException{
		File bfDir = new File(bamFileDir);
		String[] bamFiles_ = bfDir.list(new FilenameFilter(){

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".bam");
			}
			
		});
		if(bamFiles_==null || bamFiles_.length==0){
			File bf = new File(bamFileDir);
			bamFileDir = bf.getParentFile().getAbsolutePath();
			bamFiles_ = new String[] {bf.getName()};
		}
		int len = bamFiles_.length;
		//len = 1;
		for(int ii=0; ii<len; ii++){
			
		String bamFile = bamFiles_[ii];
		File bam = new File(bamFileDir+"/"+bamFile);
		File outfile = new File(bam.getParentFile(), bam.getName()+".txt");
		File outfile1 = new File(bam.getParentFile(), bam.getName()+"coref.txt");
		File outfile2 = new File(bam.getParentFile(), bam.getName()+"clusters.txt");
		SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
		SamReader samReader = null;//SamReaderFactory.makeDefault().open(new File(bamFile));

                if ("-".equals(bamFile))
		    samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(System.in));
        	else
                    samReader = SamReaderFactory.makeDefault().open(bam);


		SAMRecordIterator samIter = samReader.iterator();
		//Read the reference genome
		ArrayList<Sequence> genomes = SequenceReader.readAll(refFile, Alphabet.DNA());

		//get the first chrom
		int currentIndex = 0;
		Sequence chr = genomes.get(currentIndex);

		ArrayList<IdentityProfile1> profiles = new ArrayList<IdentityProfile1>();
		
		
		
		for(int jj=0; jj<genomes.size(); jj++){
			Sequence ref = genomes.get(jj);
			profiles.add(new IdentityProfile1(ref));
		
		}

		long totReadBase = 0, totRefBase = 0;
		int  numReads = 0;

		int numNotAligned = 0;
		int max_reads = Integer.MAX_VALUE;
		//String log = "###Read_name\tRead_length\tReference_length\tInsertions\tDeletions\tMismatches\n";
		for (int cntr=0; samIter.hasNext() && cntr < max_reads; cntr++){
			SAMRecord sam = samIter.next();

			if (pattern != null && (!sam.getReadName().contains(pattern)))
				continue;

			//make the read seq			
			Sequence readSeq = new Sequence(Alphabet.DNA(), sam.getReadString(), sam.getReadName());
			if (readSeq.length() <= 1){
				//LOG.warn(sam.getReadName() +" ignored");
				//TODO: This might be secondary alignment, need to do something about it
				continue;
			}			

			numReads ++;


			if (sam.getReadUnmappedFlag()){
				numNotAligned ++;
				continue;
			}

			int flag = sam.getFlags();


			if (sam.getMappingQuality() < qual) {
				numNotAligned ++;
				continue;
			}



			//int refPos = sam.getAlignmentStart() - 1;//convert to 0-based index
			int refIndex = sam.getReferenceIndex();

			//if move to another chrom, get that chrom
			if (refIndex != currentIndex){
				currentIndex = refIndex;
				chr = genomes.get(currentIndex);
			}

			
				HTSUtilities.identity1(chr, readSeq, sam, profiles.get(currentIndex));			

			//log+=sam.getReadName() + "\t" + profile.readBase + "\t" + profile.refBase + "\t" + profile.baseIns + "\t" + profile.baseDel + "\t" + profile.mismatch+ "\n";

			
			//numReadsConsidered ++;
			
		}		
		samReader.close();
IdentityProfile1 pr1 = profiles.get(0);
PrintWriter pw = new PrintWriter(new FileWriter(outfile));
pr1.print(pw, genomes.get(0));
pw.close();


pr1.printCoRef(outfile1);
pr1.printClusters(outfile2);

		System.out.println("========================= TOTAL ============================");
		}
		//Done

		//System.out.println(log);
	}



	

}

/*RST*
----------------------------------------------------------
*jsa.hts.errorAnalysis*: Error analysis of sequencing data
----------------------------------------------------------

*jsa.hts.errorAnalysis* assesses the error profile of sequencing data by getting the numbers
of errors (mismatches, indels etc) from a bam file. Obviously, it does not distinguish
sequencing errors from mutations, and hence consider mutations as errors. It is best to use
with the bam file from aligning sequencing reads to a reliable assembly of the sample.

<usage>

*RST*/
