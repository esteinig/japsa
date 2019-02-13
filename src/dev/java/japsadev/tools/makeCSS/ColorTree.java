package japsadev.tools.makeCSS;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import japsadev.bio.phylo.AntibioticTree;
import japsadev.bio.phylo.NCBITree;
import mdsj.MDSJ;
import pal.misc.Identifier;
import pal.tree.Node;
import pal.tree.NodeUtils;
import pal.tree.Tree;
import pal.tree.TreeUtils;
//
//

public class ColorTree {
	 private static final Logger LOG = LoggerFactory.getLogger(ColorTree.class);
	//boolean slug = true;
	boolean species = true;
	double[][] distances ;
	Identifier[]  identifiers;
	double[] startend ; // this partitions the hue space
static double maxlight = 85;
//https://journals.sagepub.com/doi/full/10.4137/EBO.S7565#_i6
public static void main(String[] args){
	try{ 
	System.err.println(getHex(240,100,30,1));
	File f = new File(args[1]);
	boolean species = args[0].equals("species");
		Tree[] tree =species?NCBITree.readTree(f):  AntibioticTree.readTree(f);
		for(int i=0; i<tree.length; i++){
			System.err.println(i);;
			ColorTree ct = new ColorTree(tree[i]);
			ct.color();
		}

	}catch(Exception exc){
		exc.printStackTrace();
		
	}
}



public void printSlug(String out) throws Exception{
	PrintWriter pw = new PrintWriter(new FileWriter(out));
	/*
	for(Iterator<String> it = this.colors.keySet().iterator(); it.hasNext();){
		String key = it.next();
		String value = colors.get(key);
		pw.println(key+"\t"+value);
		
	}*/
	pw.close();
}

 static double rand =0;
 
 
 static double[][]getMatrix(Tree tree){
	
	 int cnt = tree.getExternalNodeCount();
		// int cnt1 = cnt;
		 double[] 	idist = new double[tree.getInternalNodeCount()];
		
		double epsilon = 0;
			boolean countEdges = false;
			
			double[][] X  = new double[cnt][cnt];
			for(int i=0; i<cnt; i++){
			
				Arrays.fill(idist, 0);
				double[] dist  = new double[cnt];
				int minind =0;
				double minval = 1e9;
			    TreeUtils.computeAllDistances(tree, i, dist, idist, countEdges, epsilon);
				for(int j=0; j<cnt; j++) {
					if(j!=i && dist[j]<minval){
						minind = j;
						minval = dist[j];
					}
					X[i][j] = dist[j];
					//X.set(i, j, dist[j]);
				}
				//String nme = 	tree.getExternalNode(i).getIdentifier().getName();
				//String nme1 = 	tree.getExternalNode(minind).getIdentifier().getName();
				//System.err.println(nme+" -> "+nme1);
			}
			return X;
 }
 void color() throws Exception{
	 if(this.tree.getExternalNodeCount()==1){
		tree.getExternalNode(0).getIdentifier().setAttribute("css" ,	"#ffffff00");   // for homo sapiens
	 }else{
		 double[] startend_ = this.startend;
		 double[][] X = distances;
		 System.err.println(X.length);
		 System.err.println(identifiers.length);
	 	 color(X);
	 }
 }
 

 
// static double[] lightnessByDepth = new double[] {80,70,60,50,40,30,20,10,5,4,3,2,1}; 
 
public  void color(double[][] X) throws Exception {

	int cnt=X.length;    // number of data objects
	double[][] U=MDSJ.classicalScaling(X); // apply MDS


	//double maxnorm = 0;
	//double minnorm =1e5;
	//double maxtheta = 0;
	//double mintheta = 1e5;
	double[] norms = new double[cnt];
	double[] thetas = new double[cnt];
	SortedSet<Double> norms_set = new TreeSet<Double>();
	SortedSet<Double> thetas_set = new TreeSet<Double>();

	for(int i=0; i<cnt; i++){
		//double r = eig1[i];//U.get(i, 0);
		//double g = eig2[i];//U.get(i, 1);
		double r = U[0][i];
		double g = U[1][i];
		//double b = U.get(i, 2);
	//	norms[i] = Math.sqrt(Math.pow(r, 2) + Math.pow(g, 2)) + Math.random()*1e-5;;
	//	thetas[i] = (Math.atan2(g, r) ) + Math.random()*1e-5;
		norms[i] = r + Math.random()*1e-5;;
		thetas[i] = g+ Math.random()*1e-5;
		norms_set.add(norms[i]);
		thetas_set.add(thetas[i]);
	}
	Map<String, double[]> colors1 = new HashMap<String, double[]>();
	double normsize = norms_set.size();
	double thetasize = thetas_set.size();
	PrintWriter range = new PrintWriter(new FileWriter(new File("range.txt")));
	double maxdepth = 0;
	for(int i=0; i<cnt; i++){
		 double depth = ((Number)this.identifiers[i].getAttribute("level")).intValue();
		 if(depth>maxdepth)maxdepth = depth;
	}
	for(int i=0; i<cnt; i++){
		
		double h = ((double)norms_set.headSet(norms[i]).size())/normsize;
		double t = ((double)thetas_set.headSet(thetas[i]).size())/thetasize;
		 t = t*0.6  + 0.4;  //to make sure >0.4 saturatoin
		 double depth = ((Number)this.identifiers[i].getAttribute("level")).intValue();
		 double lightness = depth/maxdepth;
		 String hexvalue = getHex(h * 360,t * 100,lightness,1.0);
		 identifiers[i].setAttribute("css", hexvalue);
		 identifiers[i].setAttribute("cssvals", new double[] {h,t, depth});
	}
	System.err.println("max depth " +maxdepth);
	int sze1 = colors1.size();
	System.err.println(sze1);
	
	//NodeUtils.postorderSuccessor(tree.getExternalNode(0));
	for(int i=internal.length-1; i>=0;  i--){
		Node n = internal[i];
		int cc = n.getChildCount();
		double[] d = new double[]{0,0,0};
		List<String> l = new ArrayList<String>();
		Identifier id = n.getIdentifier();
		for(int j =0; j<cc; j++){
			Node child = n.getChild(j);
			Identifier c_ident = child.getIdentifier();
			double[] d1 = (double[]) c_ident.getAttribute("cssvals");
			//System.err.println(d1);
			d[0]+=d1[0];
			d[1]+=d1[1];
		}
		d[0] = d[0]/cc;
		d[1] = d[1]/cc;
		d[2] = ((Number)n.getIdentifier().getAttribute("level")).intValue();
		 double lightness = d[2]/maxdepth;
		String hexvalue = getHex(d[0] * 360,d[1] * 100,lightness,1.0);
		id.setAttribute("css", hexvalue);
			id.setAttribute("cssvals", d);
	}
	range.close();
}




//Map<String, String> colors = new HashMap<String, String>();
//Map<String, String> groups1 = new HashMap<String, String>();
//Map<String, List<String>> groups= new HashMap<String, List<String>>(); //maps slug to the group it belongs
//Map<String, double[]> colors1 = new HashMap<String, double[]>();

 
Node[] internal = new Node[0];
Tree tree; 



ColorTree(Tree tree) throws Exception{
		//colors.put("grch38",	"#ffffff00");  // transparent for human
		//slug = true;
		this.tree = tree;
		int cnt = tree.getExternalNodeCount();
		System.err.println("read tree with "+cnt);
	//Tree tree = new ReadTree(f);
	setBL(tree.getRoot(), 100, 0.5);
	Identifier[] identifier = getIdentifiers(tree);
	this.identifiers = (identifier);
	this.startend= (new double[] {0,1});
	if(cnt>1){
		distances =  getMatrix(tree);
		internal  = NodeUtils.getInternalNodes(tree.getRoot(), true);
	
	
	}
	
	
}

private static Identifier[] getIdentifiers(Tree stree) {
	Identifier[] ids = new Identifier[stree.getExternalNodeCount()];
	for(int i=0; i<ids.length; i++){
	ids[i] = stree.getIdentifier(i);
	}
	return ids;
}


private static void setBL(Node root, double d, double frac) {
	root.setBranchLength(d);
	if(!root.isLeaf()){
	int cnt = root.getChildCount();
	for(int i=0; i<cnt; i++){
		double dist = d*frac;
		dist = dist*(1+ Math.random()*rand);
		setBL(root.getChild(i), dist, frac);
	}
	}
	
}

public static String getHex(double h, double s, double l, double alpha){
	//System.err.println(String.format("%02x", 15));
	HSLColor color = new HSLColor((float) h, (float) s, (float) l, (float) alpha);
	Color rgb = color.getRGB();
	int alph = Math.round(color.getAlpha() * (float)255);
	String hex = String.format("#%02x%02x%02x%02x", rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alph);  
	return hex;
  // return "#"+ Integer.parseInt(r, 16)+ Integer.toHexString(g)+ Integer.toHexString(b);
	
}

}