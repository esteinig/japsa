#!/usr/env python

""" Collect the results of the current nested output structure of JAPSA Coverage (SpeciesTyping)

Usage: python scripts/collect_species.py <sample_id> <result_dir> <outfile>

"""

import sys

from shutil import copy
from pathlib import Path

sample = sys.argv[1]
results = sys.argv[2]
outdir = sys.argv[3]

print(f"Collecting output files for sample {sample} in {results} into: {outdir}")

Path(outdir).mkdir(parents=True, exist_ok=True)

header = None
lines = []
for f in Path(results).rglob(f"*.dat"):
    if f.stem == sample:
        print(f"Found database result file {f.name} in directory: {f.parent.name}")
        if f.parent.name.startswith(sample):
            db_name = f.parent.parent.name
        else:
            db_name = f.parent.name

        db = db_name.lstrip("unmapped.fq.gz.").rstrip(".jST")

        print(f"Database name {db} inferred from directory: {db_name}")

        copy(str(f), f"{outdir}/{db}.dat")
        


