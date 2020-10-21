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

    def get_features(self):
        return [self.turbidity, self.ph, self.chlorine, self.temperature, self.conductivity]

    def get_lossy_features(self):
        if math.isnan(self.turbidity):
            return [1, [self.ph, self.chlorine, self.temperature, self.conductivity]]
        elif math.isnan(self.ph):
            return [2, [self.turbidity, self.chlorine, self.temperature, self.conductivity]]
        elif math.isnan(self.chlorine):
            return [3, [self.turbidity, self.ph, self.temperature, self.conductivity]]
        elif math.isnan(self.temperature):
            return [4, [self.turbidity, self.ph, self.chlorine, self.conductivity]]
        else:
            return [5, [self.turbidity, self.ph, self.chlorine, self.temperature]]

    def set_feature(self, idx, val):
        if (idx == 1):
            self.turbidity = val
        elif (idx == 2):
            self.ph = val
        elif (idx == 3):
            self.chlorine = val
        elif (idx == 4):
            self.temperature = val
        elif (idx == 5):
            self.conductivity = val

    def __str__(self):
        str = "<{}>".format(self.dt)

        if self.checker is None:
            str += self.checkerid
        else:
            str += self.checker
        str += " turbidity:{},ph:{},chlorine:{},temperature:{},conductivity:{}".format(
            self.turbidity, self.ph, self.chlorine, self.temperature, self.conductivity)
        return str
