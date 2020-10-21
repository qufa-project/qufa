import csv
import logger
import re
from datetime import datetime
import arisuq

class ArisuData:
    def __init__(self):
        self.checker_names = []
        self.arisuqs = []
        self.__reader = None
        self.__drop_loss_item = False

    def load(self, path, drop_loss_item=False):
        try:
            f = open(path, "r", encoding="utf-8")
        except IOError:
            logger.error("data path not found: {}".format(path))
            return False

        self.__drop_loss_item = drop_loss_item
        self.__reader = csv.reader(f, delimiter=',', )

        self.__load_header()
        if len(self.checker_names) == 0:
            self.__load_data()
        else:
            self.__load_raw_data()

        return True

    def __load_header(self):
        row1 = next(self.__reader)
        if len(row1) == 1 and row1[0].startswith('#date'):
            return
        idx = 1
        while idx < len(row1):
            if idx + 5 <= len(row1):
                self.checker_names.append(self.__get_checker_name(row1[idx]))
            idx += 5

    def __get_checker_name(self, str):
        regex = re.compile(r'\[(.*)\]')
        mo = regex.search(str)
        return mo.group(1)

    def __load_raw_data(self):
        for row in self.__reader:
            self.__load_raw_data_row(row)

    def __load_raw_data_row(self, row):
        dt = datetime.strptime(row[0], "%Y-%m-%d %H")
        col = 1
        for idx in range(len(self.checker_names)):
            aq = arisuq.ArisuQ(idx + 1, dt, row[col:col+5])
            if not self.__drop_loss_item or not aq.is_loss:
                self.arisuqs.append(aq)
            col += 5

    def __load_data(self):
        for row in self.__reader:
            aq = arisuq.ArisuQ(row)
            self.arisuqs.append(aq)

    def save(self, path):
        with open(path, "w") as fw:
            fw.write("#date checker turbidity ph chlorine temperature conductivity\n")

            for aq in self.arisuqs:
                if not aq.checker is None:
                    checker = aq.checker
                else:
                    checker = self.checker_names[aq.checkerid - 1]
                fw.write("{}, {}, {}, {}, {}, {}, {}\n".format(aq.dt, checker, aq.turbidity, aq.ph,
                                                               aq.chlorine, aq.temperature, aq.conductivity))
