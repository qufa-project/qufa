import json
import argparse

import pandas as pd
from sklearn.preprocessing import StandardScaler

from outlier_detection import Detector


def get_data(input_fname, train_fname):
    data = pd.read_csv(input_fname)
    num_idxs = data.dtypes[data.dtypes != 'object'].index
    num_vars = [data.columns.get_loc(idx) for idx in num_idxs]

    X_numeric = data.iloc[:, num_vars]
    scaler = StandardScaler()
    scaler.fit(X_numeric)
    X_numeric = scaler.transform(X_numeric)
    
    trainset = pd.read_csv(train_fname)
    X_train = trainset.iloc[:, num_vars]
    X_train = scaler.transform(X_train)
    return X_numeric, X_train

def get_outlier_rate(X_numeric, X_train):
    detector = Detector()
    detector.fit(X_train)
    outlier_idxs = detector.extract(X_numeric)
    
    N = X_numeric.shape[0]
    outlier_rate = len(outlier_idxs) / N * 100
    outlier_rate = round(outlier_rate, 4)
    return outlier_rate


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname')
    parser.add_argument('--train_fname')
    args = parser.parse_args()
    
    X_numeric, X_train = get_data(args.input_fname, args.train_fname)
    outlier_rate = get_outlier_rate(X_numeric, X_train)
    print(outlier_rate, '%')
