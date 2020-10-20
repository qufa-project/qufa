import math
from datetime import datetime

class ArisuQ:
    is_loss = False
    checker = None

    def __init__(self, *args):
        if len(args) == 1:
            self.__init_by_row(args[0])
        else:
            self.__init_by_raw(args[0], args[1], args[2])

    def __init_by_row(self, row):
        self.dt = datetime.strptime(row[0], "%Y-%m-%d %H:%M:%S")
        self.checker = row[1]
        self.turbidity = self.__get_float(row[2])
        self.ph = self.__get_float(row[3])
        self.chlorine = self.__get_float(row[4])
        self.temperature = self.__get_float(row[5])
        self.conductivity = self.__get_float(row[6])

    def __init_by_raw(self, checkerid, dt, metrics):
        self.dt = dt
        self.checkerid = checkerid
        self.turbidity = self.__get_float(metrics[0])
        self.ph = self.__get_float(metrics[1])
        self.chlorine = self.__get_float(metrics[2])
        self.temperature = self.__get_float(metrics[3])
        self.conductivity = self.__get_float(metrics[4])

    def __get_float(self, valstr):
        try:
            val = float(valstr)
            if math.isnan(val):
                self.is_loss = True
            return val
        except ValueError:
            self.is_loss = True
            return float('nan')

    def __str__(self):
        str = "<{}>".format(self.dt)

        if self.checker is None:
            str += self.checkerid
        else:
            str += self.checker
        str += " turbidity:{},ph:{},chlorine:{},temperature:{},conductivity:{}".format(
            self.turbidity, self.ph, self.chlorine, self.temperature, self.conductivity)
        return str
