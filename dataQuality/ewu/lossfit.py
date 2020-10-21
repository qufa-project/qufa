#!/usr/bin/python3

import sys
import os
import fit_data
import logger
import getopt
import random

def usage():
    print("""\
Usage: lossfit.py <path> <result path>
""")

class LossFit:
    def run(self, path, path_res):
        data = fit_data.FitData()
        if not data.load(path):
            return 1

        data.fit()

        data.save(path_res)

        return 0

if __name__ == "__main__":
    import lossfit

    if len(sys.argv) < 3:
        usage()
        exit(1)

    logger.init("lossfit")

    if not os.path.isfile(sys.argv[1]):
        logger.error("file not found: {}".format(sys.argv[1]))
        exit(1)
    if os.path.exists(sys.argv[2]):
        logger.error("result path already exist: {}".format(argv[2]))
        exit(1)

    lf = lossfit.LossFit()
    exit(lf.run(sys.argv[1], sys.argv[2]))
