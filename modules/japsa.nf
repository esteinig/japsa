process JAPSACoverageSpecies {

    label "japsa"
    tag { id }

    publishDir "${params.outdir}/species", mode: "copy", pattern: "${id}.dat"

    input:
    tuple val(id), file(fastq)

    output:
    tuple val(id), file("${id}_results/")
    
    script:

    """
    java -Xmx8000m -cp /usr/local/bin/japsacov-1.9.5e.jar japsa.tools.bio.np.RealtimeSpeciesTypingCmd \
        --resdir=${id}_results --output=${id}.dat --fastqFile=$fastq \
        --dbpath=$params.db_path --dbs=$params.dbs --mm2_threads=$task.cpus \
        --qual=0 --fail_thresh=7 --time=0 --log=false --mm2_path=minimap2 --minCount=4 \
        --deleteUnmapped=true --writeSep=[a-z] --alignedOnly=true \
        --minCoverage=4 --minLength=1000 --reduceToSpecies=false --buildConsensus=true
    """

}

process JAPSACoverageBLAST {

    // Adopted from Dan's script

    label "japsa"
    tag { id }

    input:
    tuple val(id), file(results)

    output:
    tuple val(id), file(results)
    
    script:

    """
    files=\$(find $results -name consensus_output.fa)
    base_dir=\$PWD

    for found in \${files}; do
        cd \$(dirname \${found})
        blastn -num_threads $task.cpus -outfmt 17 -query consensus_output.fa -db $params.blast_db -out blastn_sam.out
        mv hits.index.txt blastn_sam.out.fa.index
        cd \$base_dir
    done
    """
}


process JAPSACoverageBLASTSpecies {

    // Adopted from Dan's script

    label "japsa"
    tag { id }

    input:
    tuple val(id), file(results)

    output:
    tuple val(id), file(results)
    
    script:

    """
    java -Xmx8000m -cp /usr/local/bin/japsacov-1.9.5e.jar japsa.tools.bio.np.RealtimeSpeciesTypingCmd \
        --resdir=${id}_results --output=${id}.dat --fastqFile=$fastq --dbpath=$params.db_path \
        --removeLikelihoodThresh=0 --mkTree=true --flipName=true --blast=true --tag=PI
    """
}