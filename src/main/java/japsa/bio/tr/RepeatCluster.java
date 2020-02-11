package japsa.bio.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import japsa.tools.bio.hts.VNTRLongReadsCmd;

public class RepeatCluster {
	
	static int thresh_cluster = 2; // any clusters this size or smaller are removed 
	static int thresh_indiv = 2; //any categories with this number or fewer reads is immediately discarded
	
	public static void main(String[] args){
		try{
			String[] alleles = "24:23:24:24:24:23:23:24:23:22:23:24:24:23:24:23:23:23:23:23:23:23:23:23:22:23:24:23:23:23".split(":");
			ReadAllele[] alleles1 = new ReadAllele[alleles.length];
			for(int i=0; i<alleles1.length; i++){
				alleles1 [i] = new ReadAllele("", Double.parseDouble(alleles[i]));
			}
			List<ReadAllele> l1 = Arrays.asList(alleles1);
			Number[][] n1 = VNTRLongReadsCmd.clustering(RepeatCluster.removeOutliers(l1, 2));
			Number[][] d =RepeatCluster.genotype(l1);
			if(d==null) System.err.println("null");
			else System.err.println(Arrays.asList(d[0])+";"+ Arrays.asList(d[1]));
		}catch(Exception exc){
			exc.printStackTrace();
		}
	}

	public static Number[][] genotype(List<ReadAllele> all){
		RepeatCluster rc = new RepeatCluster(all);
		return rc.geno();
	}
	
	public Number[][] geno(){
		Number[][] d = genotype();
		if(d==null || d.length==0 || d[0].length==0){
			Number[] n1 = new Number[] {Double.NaN, Double.NaN};
			Number[] n2 = new Number[] {0,0};
			return new Number[][] {n1,n2};
		}
		else return d;
	}
	
	Clusterer clust;
	Map<Double, Integer> counts = new HashMap<Double, Integer>();
	Map<Double, Integer> removed = new HashMap<Double, Integer>();
	List<Double> alleles; 
	List<DoublePoint> alleles1 ;
	List<CentroidCluster<DoublePoint>> clusters; 
	int[] size; //cluster size
	int min_ind =-1; //index of smallest cluster
	
	public RepeatCluster(List<ReadAllele> alleles){
		this.alleles = new ArrayList<Double>();
		this.alleles1 = new ArrayList<DoublePoint>();
		for(int i=0; i<alleles.size(); i++){
			this.alleles.add(alleles.get(i).copy_number);
			alleles1.add(new DoublePoint(new double[] {alleles.get(i).copy_number}));
		}
		getcounts(counts, removed, this.alleles, thresh_indiv);
		clust = new KMeansPlusPlusClusterer(2);
//		clust = new FuzzyKMeansClusterer((int) 2, 2.0);
	}
	
	private Number[] extractGenotypes(){
		int[] res = null;
		Number[] keys = counts.keySet().toArray(new Integer[counts.size()]);
		if(keys.length==2) return keys;
		else if(keys.length==0) return new Number[] {Double.NaN, Double.NaN};
		else if (keys.length==1) return new Number[] {keys[0], keys[0]};
		else return null;	
	}
 private  void  kmeans(){
		 clusters = clust.cluster(alleles1);
		 size = new int[clusters.size()];
		 int minsize = Integer.MAX_VALUE;
		 min_ind = -1;
		 for(int i=0; i<clusters.size(); i++){
			CentroidCluster cc =  clusters.get(i);
			size[i] = cc.getPoints().size();
			if(size[i] < minsize){
				minsize = size[i];
				min_ind = i;
			}
		 }
		
	}
 
 private Number[][] averages(){
	 Number[] res = new Number[clusters.size()];
	 Number[] cnt = new Number[clusters.size()];
	 for(int i=0; i<res.length; i++){
		// System.err.println(clusters.get(i).getPoints());
		 res[i] = ((DoublePoint)((CentroidCluster) clusters.get(i)).getCenter()).getPoint()[0];
		 
		cnt[i] =  ((CentroidCluster) clusters.get(i)).getPoints().size();
	 }
	return new Number[][] {res,cnt};
 }
	private  void removeMinCluster(){
		List<DoublePoint> dpl = clusters.get(min_ind).getPoints();
		Set<Integer> toremove = new TreeSet<Integer>();
		for(int i=0; i<dpl.size(); i++){
			toremove.add(this.alleles1.indexOf(dpl.get(i)));
		}
	
		for(int i=this.alleles.size()-1;i>=0;  i--){
			if(toremove.contains(i)){
				alleles.remove(i);
				alleles1.remove(i);
			}
		}
		this.getcounts(counts, removed, alleles, thresh_indiv);
	}
	
	
	/*default thresh_cluster is 2 and thresh_indiv is 2 */
	 private Number[][] genotype(){
		  Number[][] genotypes = getGenotypes();
		  if(genotypes[0].length<=2) return genotypes;
		 
		 kmeans();
		
	
		  while(size[min_ind]<=thresh_cluster){	
			  this.removeMinCluster();
			  genotypes = getGenotypes();
			  if(genotypes[0].length<=2) return genotypes;
			  kmeans();
		  }
		  if(genotypes[0].length<=2) return genotypes;
		  else {
			  return averages();
			  
		  }
		// now filter on proportion
		 // res = cbind(km$centers, km$size)
		 // dimnames(res)[[2]] = c("centers" , "size" )
		 // list(geno=geno,counts=counts,cluster=res)
		}
	
	
	
	
    private Number[][] getGenotypes() {
		// TODO Auto-generated method stub
    	Number[] n = counts.keySet().toArray(new Double[counts.size()]);
		Number[] n1 = new Number[n.length];
		for(int i=0; i<n1.length; i++){
			n1[i] = counts.get(n[i]);
		}
		return new Number[][] {n,n1};
	}

    public static List<ReadAllele> removeOutliers(List<ReadAllele>alleles, int thresh1){
    Map<Double, Integer> counts1 = new HashMap<Double, Integer>();
  	  for(int i=0;i<alleles.size(); i++){
  		  Integer cnt = counts1.containsKey(alleles.get(i).copy_number) ? counts1.get(alleles.get(i).copy_number) : 0;
  		  counts1.put(alleles.get(i).copy_number, cnt+1);
  	  }
  	Map<Double, Integer> removed = new HashMap<Double, Integer>();
  	List<ReadAllele> reads_to_keep = new ArrayList<ReadAllele>();
 // 	List<ReadAllele> reads_to_remove = new ArrayList<ReadAllele>();
  	 for(Iterator<Double> it = counts1.keySet().iterator(); it.hasNext();){
		  Double key = it.next();
		  Integer value = counts1.get(key);
		//  List<ReadAllele> reads_to_change = reads_to_keep;
		  if(value < thresh1){
			  removed.put(key, value);
		//	  reads_to_change = reads_to_remove;
		  }else{
			 for(int i=0; i<alleles.size(); i++){
					  if(Math.abs(alleles.get(i).copy_number - key.doubleValue()) < 1e-5){
						  reads_to_keep.add(alleles.get(i));
					  }
			 }
		  }
		  
	  }
  	  return reads_to_keep;
    }
    
	static void getcounts(Map<Double, Integer> counts, Map<Double, Integer> removed,  List<Double> alleles, int thresh1){
    	counts.clear();
    	removed.clear();
    	Map<Double, Integer> counts1 = new HashMap<Double, Integer>();
    	  for(int i=0;i<alleles.size(); i++){
    		  Integer cnt = counts1.containsKey(alleles.get(i)) ? counts1.get(alleles.get(i)) : 0;
    		  counts1.put(alleles.get(i), cnt+1);
    	  }
    	  
		  
		  for(Iterator<Double> it = counts1.keySet().iterator(); it.hasNext();){
			  Double key = it.next();
			  Integer value = counts1.get(key);
			  if(value < thresh1){
				  removed.put(key, value);
				 
			  }else{
				  counts.put(key,value);
			  }
		  }
		}
	
	
}
