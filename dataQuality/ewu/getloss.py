#!/usr/bin/python3

import sys
import arisu_data
import logger
import getopt

def usage():
    print("""\
Usage: getloss.py [-e <max error>] <path> [<original>]
  -e <max_error>: maximum error rate to original value(0~1)
""")

class GetLoss:
    def __init__(self, err_rate):
        self.__err_rate = err_rate

    def run(self, path, path_org):
        data = arisu_data.ArisuData()
        if not data.load(path):
            return 1

        data_org = None
        if not path_org is None:
            data_org = arisu_data.ArisuData()
            if not data_org.load(path_org):
                return 1

        if not data_org is None and len(data.arisuqs) != len(data_org.arisuqs):
            logger.error("original data is mismatched")
            return 1

        idx = 0
        n_loss = 0

        for aq in data.arisuqs:
            aq_org = None
            if not data_org is None:
                aq_org = data_org.arisuqs[idx]
            if self.__is_lossy_arisuq(aq, aq_org):
                n_loss += 1
            idx += 1

        n_totals = len(data.arisuqs)
        print("Loss rate: {}% ({}/{})".format(round(n_loss / n_totals * 100, 4), n_loss, n_totals))
        return 0

    def __is_lossy_arisuq(self, aq, aq_org):
        if aq.is_loss:
            return True
        if not aq_org is None:
            if aq_org.is_loss:
                return True
            fs1 = aq.get_features()
            fs2 = aq_org.get_features()

            for i in range(0, 5):
                if abs(fs1[i] - fs2[i]) > fs1[i] * self.__err_rate:
                    return True
        return False

err_rate = 0.3

def parseArgs():
    global      err_rate, args

    try:
        opts, args = getopt.getopt(sys.argv[1:], "e:")
    except getopt.GetoptError:
        return False

    for o, a in opts:
        if o == '-e':
            err_rate = float(a)

    if err_rate < 0 or err_rate > 1:
        logger.error("invalid max error rate: {}".format(err_rate))
        return False
    if len(args) < 1:
        return False

    return True
if __name__ == "__main__":
    import getloss

    logger.init("getloss")

    if not parseArgs():
        usage()
        exit(1)

    path_org = None
    if len(args) == 2:
        path_org = args[1]

    gl = getloss.GetLoss(err_rate)
    exit(gl.run(args[0], path_org))
