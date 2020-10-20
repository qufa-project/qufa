#!/usr/bin/python3

import sys
import os
import lossy_data
import logger
import getopt
import random

def usage():
    print("""\
Usage: makeloss.py [-r lossrate] <path> <result path>
""")

class MakeLoss:
    def run(self, path, path_res, lossrate):
        data = lossy_data.LossyData()
        if not data.load(path):
            return 1

        data.go_lossy(lossrate)

        data.save(path_res)

        return 0

lossrate = 0.01

def parseArgs():
    global      lossrate, args

    try:
        opts, args = getopt.getopt(sys.argv[1:], "r:")
    except getopt.GetoptError:
        return False

    for o, a in opts:
        if o == '-r':
            lossrate = float(a)

    if lossrate < 0 or lossrate > 1:
        logger.error("invalid loss rate: {}".format(lossrate))
        return False
    if len(args) != 2:
        return False

    return True

if __name__ == "__main__":
    import makeloss

    logger.init("makeloss")

    if not parseArgs():
        usage()
        exit(1)

    if not os.path.isfile(args[0]):
        logger.error("file not found: {}".format(args[0]))
        exit(1)
    if os.path.exists(args[1]):
        logger.error("result path already exist: {}".format(args[1]))
        exit(1)

    ml = makeloss.MakeLoss()
    exit(ml.run(args[0], args[1], lossrate))
