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

/**************************     REVISION HISTORY    **************************
 * 18/10/2013 - Minh Duc Cao: Created                                        
 *  
 ****************************************************************************/

package japsa.tools.bio.np;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;
import japsa.bio.hts.scaffold.AlignmentRecord;
import japsa.bio.hts.scaffold.Contig;



@Deployable(
		scriptName = "jsa.np.flankDetect",
		scriptDesc = "Detect flanking sequences from both ends of nanopore reads. Results will be printed to stdout while logs to stderr."
		)
public class FlankSeqsDetectorCmd extends CommandLine{
    private static final Logger LOG = LoggerFactory.getLogger(FlankSeqsDetectorCmd.class);

	public FlankSeqsDetectorCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		addString("flankFile",null,"Flank sequences file, maximum 2 sequences. The first one determines the true flank (LTR)",true);
		addString("refFile","","Reference sequences (human genome)");
		addString("bamFile",null,"SAM/BAM file generated by aligning ONT reads to the ref and flank sequences",true);
		addDouble("qual", 1, "Mininum quality");
		addInt("insert", 10, "Minimum length of the integration site");
		addInt("tips", 20, "Maximum percentage of the overhangs compared to the corresponding flanking sequence");
		addInt("distance", 3, "Distance for DBSCAN clustering algorithm.");
		addDouble("cover", 80, "Mininum percentage of flank sequence coverage for a valid alignment");

		addStdHelp();	
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		/*********************** Setting up script ****************************/		 
		/*********************** Setting up script ****************************/		
		CommandLine cmdLine = new FlankSeqsDetectorCmd();		
		args = cmdLine.stdParseLine(args);
		/**********************************************************************/
		String 	flankSeqsFile= cmdLine.getStringVal("flankFile"),
				refSeqsFile= cmdLine.getStringVal("refFile");
		String bamFile = cmdLine.getStringVal("bamFile");
		double 	qual = cmdLine.getDoubleVal("qual"),
				flkCov = cmdLine.getDoubleVal("cover");
		int insertLength = cmdLine.getIntVal("insert"),
			distance = cmdLine.getIntVal("distance"),
			tipsPercentage = cmdLine.getIntVal("tips");
		
		SequenceReader seqReader = SequenceReader.getReader(flankSeqsFile);
		Sequence seq;
		HashMap<String, Contig> refSeqs = new HashMap<>();
		ArrayList<Contig> flankSeqs=new ArrayList<>();
							
		int index=0;
		while ((seq = seqReader.nextSequence(Alphabet.DNA())) != null)
			flankSeqs.add(new Contig(index++,seq));
		seqReader.close();
		
		if(!refSeqsFile.isEmpty()){
			seqReader = SequenceReader.getReader(refSeqsFile);
			index=0;
			while ((seq = seqReader.nextSequence(Alphabet.DNA())) != null)
				refSeqs.put(seq.getName(), new Contig(index++,seq));
			seqReader.close();
		}
		
		if(flankSeqs.size() > 2){
			LOG.error("More than 2 sequences!");
			System.exit(1);
		}
		
		SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
		SamReader reader = SamReaderFactory.makeDefault().open(new File(bamFile));
		SAMRecordIterator iter = reader.iterator();

		SAMRecord curSAMRecord=null;
		AlignmentRecord curAlnRecord=null;
		FlankRecord fr=null;
		String readID = "";
		List<FlankRecord> records=new ArrayList<>();
		/**********************************************************************
		 ********************** Junctions detection ***************************
		 **********************************************************************/
		while (iter.hasNext()) {
	
			try {
				curSAMRecord = iter.next();
			}catch(Exception e) {
//				LOG.warn("Ignore one faulty SAM record: \n {}", e.getMessage());
				continue;
			}
			
			if (curSAMRecord.getReadUnmappedFlag() || curSAMRecord.getMappingQuality() < qual || curSAMRecord.isSecondaryAlignment()){		
//				LOG.info("Ignore record! Unmapped, low-quality or secondary alignment from {}...", curSAMRecord.getReadName());
				continue;		
			}
								
			if (!readID.equals(curSAMRecord.getReadName())){
				//output prev
				if(fr!=null)
					records.add(new FlankRecord(fr));
				
					
				//update for next
				readID = curSAMRecord.getReadName();
				fr=new FlankRecord(readID);
			}
			//adding info to current FlankRecord
			Contig ctg=null;
			String refName = curSAMRecord.getReferenceName();
			if(refSeqs.containsKey(refName)){
				ctg=refSeqs.get(refName);
				curAlnRecord=new AlignmentRecord(curSAMRecord, ctg);

				if(curAlnRecord.readAlignmentEnd()-curAlnRecord.readAlignmentStart() < insertLength){
//					LOG.info("Ignore record! Integration size too short: {}", curAlnRecord.toString());
					continue;
				}
				if(fr.refRec==null||fr.refRec.qual < curAlnRecord.qual)
					fr.refRec=curAlnRecord;
				else if(fr.refRec.qual == curAlnRecord.qual){
//					LOG.info("Ignore record! Confusing alignment on {}:\n {} \n {}", refName, curAlnRecord.toString(), fr.refRec);
					fr.refRec=null;
					continue;
				}
				
			}else{
				
				if(flankSeqs.get(0).getName().equals(refName)){
					ctg=flankSeqs.get(0);
					curAlnRecord=new AlignmentRecord(curSAMRecord, ctg);
					if(curAlnRecord.refEnd-curAlnRecord.refStart < (double)flkCov*ctg.length()/100.0)
						continue;
					//not too far from the tip of read
					else if(Math.min(-curAlnRecord.readAlignmentEnd()+curAlnRecord.readLength, curAlnRecord.readAlignmentStart()) > (double)ctg.length()*tipsPercentage/100.0){
						continue;
					}
					if(fr.f0Rec==null||fr.f0Rec.qual < curAlnRecord.qual)
						fr.f0Rec=curAlnRecord;
					else if(fr.f0Rec.qual == curAlnRecord.qual){
//						LOG.info("Ignore record! Confusing alignment on {}:\n {} \n {}", refName, curAlnRecord.toString(), fr.refRec);
						fr.f0Rec=null;
						continue;
					}
					
				}else if(flankSeqs.size()>1 && flankSeqs.get(1).getName().equals(refName)){
					ctg=flankSeqs.get(1);
					curAlnRecord=new AlignmentRecord(curSAMRecord, ctg);
					if(curAlnRecord.refEnd-curAlnRecord.refStart < (double)flkCov*ctg.length()/100.0)
						continue;
					//not too far from the tip of read
					else if(Math.min(-curAlnRecord.readAlignmentEnd()+curAlnRecord.readLength, curAlnRecord.readAlignmentStart()) > (double)ctg.length()*tipsPercentage/100.0){
						continue;
					}
					if(fr.f1Rec==null||fr.f1Rec.qual < curAlnRecord.qual)
						fr.f1Rec=curAlnRecord;
					else if(fr.f1Rec.qual == curAlnRecord.qual){
//						LOG.info("Ignore record! Confusing alignment on {}:\n {} \n {}", refName, curAlnRecord.toString(), fr.refRec);
						fr.f1Rec=null;
						continue;
					}
				}
//				else
//					LOG.error("Sequence {} not found, ignored!", refName);
			
					
			}
			
		}// while
		//last time adding
		if (fr!=null)
			records.add(new FlankRecord(fr));
		
		
		iter.close();
		/**********************************************************************
		 ********************** DBSCAN clustering *****************************
		 **********************************************************************/		
		List<DoublePoint> points = new ArrayList<DoublePoint>();
		HashMap<Integer, List<FlankRecord>> tf2record = new HashMap<>(); //map each unique TF value to a list of records
		int f1=0, f2=0, f12=0, f0=0;
		System.err.printf("#Read\tReference\t%s\t%s\n",flankSeqs.get(0).getName(),flankSeqs.size()>1?flankSeqs.get(1).getName():"NA");
		for(int i=0;i<records.size();i++){
			FlankRecord frec = records.get(i);
			System.err.printf("%s\t%d\t%d\t%d\n", frec.readID, frec.refRec!=null?1:0, frec.f0Rec!=null?1:0,frec.f1Rec!=null?1:0);
			if(frec.refRec==null)
				continue;
			else if(frec.f0Rec!=null && frec.f1Rec!=null){
				f12++;
			}else if(frec.f0Rec!=null){
				f1++;
			}else if(frec.f1Rec!=null){
				f2++;
				continue;
			}else{
				f0++;
				continue;
			}
			
			frec.calculateTF(flankSeqs.size()==2);
			if(frec.trueFlank>=0){
				if(!tf2record.containsKey(frec.trueFlank)){
					tf2record.put(frec.trueFlank, new ArrayList<>());
					points.add(new DoublePoint(new double[]{frec.trueFlank}));
				}
				tf2record.get(frec.trueFlank).add(frec);
			}
//			else
//				System.err.printf("Ignore clustering of: %s\n", frec.toString());

		}
		System.err.println("====================================================");
		System.err.printf("Number of aligned reads with both flank: %d\n", f12);
		System.err.printf("Number of aligned reads with only flank %s: %d\n", flankSeqs.get(0).getName(), f1);
		if(flankSeqs.size()>1)
			System.err.printf("Number of aligned reads with only flank %s: %d\n", flankSeqs.get(1).getName(), f2);
		System.err.printf("Number of aligned reads with no flank: %d\n", f0);
		System.err.println("====================================================");
		
		//minPts=0 to report every clusters, even ones with only 1 point
		DBSCANClusterer dbscan = new DBSCANClusterer(distance, 0, (a,b)->Math.abs(a[0]- b[0]));
		List<Cluster<DoublePoint>> cluster = dbscan.cluster(points);
		for(int i=0;i<cluster.size();i++) {
			Cluster<DoublePoint> c=cluster.get(i);
			TFCluster tfCluster=new TFCluster();
			for(DoublePoint p:c.getPoints()){ 
				int tf=(int)p.getPoint()[0];
				tfCluster.set(tf, tf2record.get(tf).size());
				for(FlankRecord rec:tf2record.get(tf))
					rec.setTFCluster(tfCluster);
			}

		}
		System.out.println("#ReadID\tTarget\tStart\tEnd\tStrand\tJunction\tClusterID\tCount\tMin\tMax\tMod\tR50(bp)\tR80(bp)");
		for(FlankRecord rec:records)
			System.out.println(rec.print());
	}



}
//To maintain distribution for each cluster detected.
class TFCluster{
	private static final AtomicInteger count = new AtomicInteger(0); 
	private final int ID;
	private int size;
	HashMap<Integer, Integer> distribution;
	
	TFCluster(){
		ID=count.incrementAndGet();
		distribution=new HashMap<>();
		size=0;
	}
	public int getId(){
		return ID;
	}
	public void set(int value, int count){
		if(distribution.containsKey(value)){
			System.err.println("Warning: "+value+" already present!");;
		}
		else
			distribution.put(value, count);
		size+=count;
	}
	public String toString(){
//		DescriptiveStatistics stats = new DescriptiveStatistics(values.stream().mapToDouble(i->i).toArray());

		Set<Integer> keys=distribution.keySet();
		
		int min=Collections.min(keys), 
			max=Collections.max(keys);
		final int max_values=Collections.max(distribution.values());
		//if there is more than 1 modes, take the average
		double rmode=keys.stream().filter(k->(distribution.get(k)==max_values)).mapToDouble(k->k).average().orElse(Double.NaN);
		int mod=(int) Math.round(rmode);
		//calculate rX = minimal range from the mode making up X% of cluster size. 
		// E.g. r50=2 meaning values from [mod-2,mod+2] making up 50% of the cluster
		int r50=-1, r80=-1;
		int count=distribution.containsKey(mod)?distribution.get(mod):0;
		for(int i=0; i < max-min+1; i++){
			if(r50<0 && count>=.5*size)
				r50=i;
			if(r80<0 && count>=.8*size){
				r80=i;
				break;
			}
			count+=distribution.containsKey(mod+i)?distribution.get(mod+i):0;
			count+=distribution.containsKey(mod-i)?distribution.get(mod-i):0;
		}
		
		return getId()+"\t"+size+"\t"+min+"\t"+max+"\t"+(int)mod+"\t"+(r50<0?"NA":r50)+"\t"+(r80<0?"NA":r80);

	}
}
// A record contains alignments to Human reference and flank sequences
// The 3'-LTR junction site (trueFlank) is induced here
class FlankRecord{
	String readID;
	AlignmentRecord f0Rec, f1Rec, refRec;
	int trueFlank;
	TFCluster cluster;
	FlankRecord(String readID){
		this.readID=readID;
		f0Rec=f1Rec=refRec=null;
		trueFlank=-1;
		cluster=null;
	}
	FlankRecord(FlankRecord rec){
		readID=rec.readID;
		f0Rec=rec.f0Rec;
		f1Rec=rec.f1Rec;
		refRec=rec.refRec;
		trueFlank=rec.trueFlank;
		cluster=rec.cluster;
	}
	public void setTFCluster(TFCluster cluster){
		this.cluster=cluster;
	}
	public String toString(){
		String retval = readID+"\t";
		if(f0Rec!=null)
			retval+=f0Rec.readStart+"\t"+f0Rec.readEnd+"\t";
		else retval+="NA\tNA\t";
		
		if(refRec!=null)
			retval+=refRec.readStart+"\t"+refRec.readEnd+"\t";
		else retval+="NA\tNA\t";
		
		if(f1Rec!=null)
			retval+=f1Rec.readStart+"\t"+f1Rec.readEnd+"\t";
		else retval+="NA\tNA\t";
		
		return retval+trueFlank;
	} 
	//call to calculate true flank
	public void calculateTF(boolean bothFlank){
		if((bothFlank && f1Rec==null))
			return;
		
		if(refRec!=null && f0Rec!=null){
			int t0=(refRec.readStart-f0Rec.readStart)*(refRec.readStart-f0Rec.readEnd),
					t1=(refRec.readEnd-f0Rec.readStart)*(refRec.readEnd-f0Rec.readEnd);
				
				trueFlank=(t0<t1?refRec.refStart:refRec.refEnd);
		}
	}
	public String print(){
		String retval = readID+"\t";
		
		if(refRec!=null)			
			retval+=refRec.contig.getName()+"\t"+refRec.refStart+"\t"+refRec.refEnd+"\t"+(refRec.strand?"+":"-")+"\t";		
		else
			retval+="NA\tNA\tNA\tNA\t";		

		return retval+""+(trueFlank<0?"NA":trueFlank)+"\t"+(cluster==null?"NA\tNA\tNA\tNA\tNA\tNA\tNA":cluster.toString());
	}
}
