class ArisuQ:
    is_loss = False

    def __init__(self, checkerid, dt, metrics):
        self.dt = dt
        self.checkerid = checkerid
        self.turbidity = self.__get_float(metrics[0])
        self.ph = self.__get_float(metrics[1])
        self.chlorine = self.__get_float(metrics[2])
        self.temperature = self.__get_float(metrics[3])
        self.conductivity = self.__get_float(metrics[4])

    def __get_float(self, valstr):
        try:
            return float(valstr)
        except ValueError:
            self.is_loss = True
            return float('nan')

    def __str__(self):
        return "<{}>{} turbidity:{},ph:{},chlorine:{},temperature:{},conductivity:{}".format(
            self.dt, self.checkerid, self.turbidity, self.ph, self.chlorine, self.temperature, self.conductivity)
