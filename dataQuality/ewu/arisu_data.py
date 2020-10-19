import csv
import logger
import re
from datetime import datetime
import arisuq

class ArisuData:
    checker_names = []
    arisuqs = []

    __reader = None

    def __init__(self):
        pass

    def load(self, path):
        try:
            f = open(path, "r", encoding="utf-8")
        except IOError:
            logger.error("data path not found: {}".format(path))
            return False

        self.__reader = csv.reader(f, delimiter=',', )

        self.__load_header()
        self.__load_data()

        return True

    def __load_header(self):
        row1 = next(self.__reader)
        idx = 1
        while idx < len(row1):
            if idx + 5 <= len(row1):
                self.checker_names.append(self.__get_checker_name(row1[idx]))
            idx += 5

    def __get_checker_name(self, str):
        regex = re.compile(r'\[(.*)\]')
        mo = regex.search(str)
        return mo.group(1)

    def __load_data(self):
        for row in self.__reader:
            self.__load_data_row(row)

    def __load_data_row(self, row):
        dt = datetime.strptime(row[0], "%Y-%m-%d %H")
        col = 1
        for idx in range(len(self.checker_names)):
            self.arisuqs.append(arisuq.ArisuQ(idx + 1, dt, row[col:col+5]))
            col += 5

