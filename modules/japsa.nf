process JAPSACoverageSpecies {

    // uses jcov-species wrapper inside container

    label "japsa"
    tag { id }

    publishDir "${params.outdir}/species", mode: "copy", pattern: "${id}.dat"

    input:
    tuple val(id), file(fastq)

    output:
    file("${id}.dat")
    
    script:

    """
    java -Xmx$task.memory -cp /usr/local/bin/japsacov-1.9.5e.jar \
        --resdir=${id}_results --output=${id}.dat --fastqFile=$fastq \
        --dbpath=$params.db_path --dbs=$params.dbs --mm2_threads=$task.threads \
        --qual=0 --fail_thresh=7 --time=0 --log=false --mm2_path=minimap2 --minCount=4 \
        --deleteUnmapped=true --writeSep=[a-z] --alignedOnly=true \
        --minCoverage=4 --minLength=1000 --reduceToSpecies=false --buildConsensus=true
    """

}