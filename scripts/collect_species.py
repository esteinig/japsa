#!/usr/env python

""" Collect the results of the current nested output structure of JAPSA Coverage (SpeciesTyping)

Usage: python scripts/collect_species.py <sample_id> <result_dir> <outfile>

"""

import sys

from pathlib import Path

sample = sys.argv[1]
results = sys.argv[2]
output = sys.argv[3]

result_path = Path(results)
output_file = Path(output)

print(f"Collecting output files for sample {sample} in {result_path} into: {output_file}")

header = None
for f in result_path.rglob(f"*.dat"):
    if f.stem == sample:
        print(f"Found database result file: {f}")
        if f.parent.name.startswith(sample):
            db_name = f.parent.parent.name
        else:
            db_name = f.parent.name

        db = db_name.lstrip("unmapped.fq.gz").rstrip(".jST")

        with f.open("r") as data_file, f.open("a") as out_file:
            for line in data_file:
                if header is None:
                    # First line is header
                    out_file.write(line.strip() + f"\tDatabase\n")
                    header = line
                else:
                    out_file.write(line.strip() + f"\t{db}\n")
        



