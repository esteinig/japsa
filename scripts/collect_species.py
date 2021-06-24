#!/usr/env python

""" Collect the results of the current nested output structure of JAPSA Coverage (SpeciesTyping)

Usage: python scripts/collect_species.py <sample_id> <result_dir> <outfile>

"""

import sys

from pathlib import Path

sample = sys.argv[1]
results = sys.argv[2]
output = sys.argv[3]

print(f"Collecting output files for sample {sample} in {results} into: {output}")

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

        with f.open("r") as data_file:
            for line in data_file:
                if header is None:
                    header = line.strip() + "\tDatabase\n"
                else:
                    lines.append(line.strip() + f"\t{db}\n")

with Path(output).open("w") as out:
    out.write(header)
    for line in lines:
        out.write(line)


