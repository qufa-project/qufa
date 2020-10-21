from sklearn import linear_model
import arisu_data

class FitData(arisu_data.ArisuData):
    def fit(self):
        set_feature, set_target = self.__get_features_target()

        regs = []
        for idx in range(5):
            regs.append(linear_model.LinearRegression())
            regs[idx].fit(set_feature[idx], set_target[idx])

        for aq in self.arisuqs:
            if not aq.is_loss:
                continue
            idx, features = aq.get_lossy_features()
            est = regs[idx - 1].predict([features])
            aq.set_feature(idx, est[0])

    def __get_features_target(self):
        set_feature = [[], [], [], [], []]
        set_target = [[], [], [], [], []]

        for aq in self.arisuqs:
            if aq.is_loss:
                continue
            features = aq.get_features()
            set_feature[0].append(features[1:])
            set_target[0].append(features[0])
            set_feature[1].append(features[0:1] + features[2:])
            set_target[1].append(features[1])
            set_feature[2].append(features[0:2] + features[3:])
            set_target[2].append(features[2])
            set_feature[3].append(features[0:3] + features[4:])
            set_target[3].append(features[3])
            set_feature[4].append(features[0:4])
            set_target[4].append(features[4])

        return [set_feature, set_target]
