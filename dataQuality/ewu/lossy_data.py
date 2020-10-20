import arisu_data
import random

class LossyData(arisu_data.ArisuData):
    def go_lossy(self, lossrate):
        self.ntotals = len(self.arisuqs)
        nloss = int(lossrate * self.ntotals)

        for idx in range(nloss):
            self.__insert_loss()

    def __insert_loss(self):
        while True:
            idx_loss = int(random.random() * self.ntotals)
            aq = self.arisuqs[idx_loss]
            if not aq.is_loss:
                field = random.randint(1, 5)
                if field == 1:
                    aq.turbidity = float('nan')
                elif field == 2:
                    aq.ph = float('nan')
                elif field == 3:
                    aq.chlorine = float('nan')
                elif field == 4:
                    aq.temperature = float('nan')
                else:
                    aq.conductivity = float('nan')
                aq.is_loss = True
                return
