#!/usr/bin/env nextflow

import java.nio.file.Paths
import groovy.io.*

nextflow.enable.dsl=2

params.outdir = "jcov_species"
params.module = "species"
params.fastq = "*.fastq"
params.db_path = ""                               // must be actual path
params.dbs = "Human:plasmids:Bacteria:archaea_2019Feb:viral_2019Jan:fungi_2019feb:invertebrate_2019Feb:protozoa_2019Feb"


if (params.db_path == "") {
    println("Database path needs to be specified")
    System.exit(0)
}

def speciesMessage() {

    log.info"""
    ============================================
          J C O V - S P E C I E S  v${version}
    ============================================

    Fastq files:             ${params.fastq}
    Output directory:        ${params.outdir}
    Database path:           ${params.db_path}
    Databases:               ${params.dbs}
    ============================================

    """.stripIndent()
}

include { JAPSACoverageSpecies } from './modules/sketchy'

workflow {

    if (params.module == "species"){
        speciesMessage()
        channel.fromPath("${params.fastq}", type: 'file').map { tuple(it.simpleName, it) } | JAPSACoverageSpecies
    } 

    if (params.module == 'resistance') {
        ont = channel.fromPath("${params.fastq}", type: 'file').map { tuple(it.simpleName, it) }

    }
   

}
