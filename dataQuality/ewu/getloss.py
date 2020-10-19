#!/usr/bin/python3

import sys
import arisu_data
import logger

def usage():
    print("""\
Usage: getloss.py <path>
""")

class GetLoss:
    def run(self, path):
        data = arisu_data.ArisuData()
        if not data.load(path):
            return 1

        n_loss = 0
        for aq in data.arisuqs:
            if aq.is_loss:
                n_loss += 1

        n_totals = len(data.arisuqs)
        print("Loss rate: {}% ({}/{})".format(round(n_loss / n_totals * 100, 4), n_loss, n_totals))
        return 0

if __name__ == "__main__":
    import getloss

    if len(sys.argv) < 2:
        usage()
        exit(1)

    logger.init("getloss")
    
    gl = getloss.GetLoss()
    exit(gl.run(sys.argv[1]))
