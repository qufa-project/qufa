# -*- coding: utf-8 -*-
import os
import sys
import csv
import random
import argparse

from tqdm import tqdm
import pandas as pd
import numpy as np
from sklearn.preprocessing import MinMaxScaler
from sklearn.model_selection import train_test_split
from sklearn import metrics

import torch
import torch.nn as nn
import torch.nn.init as weight_init
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from torch.optim.lr_scheduler import ReduceLROnPlateau


parser = argparse.ArgumentParser(description='check text quality in data')
parser.add_argument('--data_fname', metavar='DIR',
        help='file name to test text quality [default: ./data/box_office.csv]',
        default='./data/box_office.csv')

def set_seed():
    seed = 128
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)

class TrainDataset(Dataset):
    def __init__(self, data):
        self.data = data
        self.len = len(self.data)

    def __getitem__(self, index):
        x = torch.tensor(self.data[index,:], dtype=torch.float32)
        return x

    def __len__(self):
        return self.len

class TestDataset(Dataset):
    def __init__(self, data):
        self.data = data
        self.len = len(self.data)

    def __getitem__(self, index):
        x = torch.tensor(self.data[index,:-1], dtype=torch.float32)
        y = torch.tensor(self.data[index,-1], dtype=torch.float32)
        return x, y

    def __len__(self):
        return self.len


class AutoEncoder(nn.Module):
    def __init__(self, input_size, drop_rate=0.5):
        super(AutoEncoder, self).__init__()
        self.drop_rate = drop_rate
        if self.drop_rate > 0:
            self.drop = nn.Dropout(drop_rate)
        
        self.encode_w1 = nn.Linear(input_size, 64)
        self.encode_w2 = nn.Linear(64, 32)
        self.decode_w1 = nn.Linear(32, 64)
        self.decode_w2 = nn.Linear(64, input_size)
    
    def encoder(self, x):
        x = self.encode_w1(x)
        x = torch.relu(x)
        x = self.encode_w2(x)
        x = torch.relu(x)
        if self.drop_rate:
            x = self.drop(x)
        return x

    def decoder(self, x):
        x = self.decode_w1(x)
        x = torch.relu(x)
        x = self.decode_w2(x)
        return x

    def forward(self, x):
        x = self.encoder(x)
        x = self.decoder(x)
        return x

def make_anomalies(data, ratio=.2):
    random.seed(128)
    label = []
    for idx in range(data.shape[0]):
        r = random.uniform(0, 1)
        if r < ratio:
            rand_cols = random.sample(range(0, data.shape[1]-1), 5)
            for rand_col in rand_cols:
                if data[idx][rand_col] == 0:
                    data[idx][rand_col] += 1.
                else:
                    data[idx][rand_col] *= 5.
            label.append(1)
        else:
            label.append(0)
    label = np.expand_dims(label, axis=1)
    data = np.concatenate((data, label), axis=1)
    return data


def train(input_size, train_loader, val_loader, device):
    num_epochs = 15

    model = AutoEncoder(input_size=input_size)
    model.to(device)

    criterion = nn.MSELoss(reduction='mean')
    optimizer = optim.Adam(model.parameters(), lr=0.0001)
    scheduler = ReduceLROnPlateau(optimizer, mode='min', factor=0.5, patience=3, verbose=True)
    
    for epoch in range(1, num_epochs + 1):
        train_loss, t_cnt = 0., 0.
        print('\nepoch : {}'.format(epoch))
        model.train()
        for x in tqdm(train_loader):
            inputs = x.to(device)
            outputs = model(inputs)
            loss = criterion(outputs, inputs)
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            t_cnt += 1.
            train_loss += loss.item()
        train_loss /= t_cnt
        print('train loss : {:.4f}'.format(train_loss))
        model.eval()
        with torch.no_grad():
            val_loss, v_cnt = 0., 0.
            for x in val_loader:
                inputs = x.to(device)
                outputs = model(inputs)
                loss = criterion(outputs, inputs)
                v_cnt += 1.
                val_loss += loss.item()
            val_loss /= v_cnt
        scheduler.step(val_loss)
        print('val loss : {:.4f}'.format(val_loss))

    return model

def get_diff(model, data_loader, device):
    diffs = []
    model.eval()
    with torch.no_grad():
        for d in tqdm(data_loader):
            if len(d) == 2:
                x, y = d
            else:
                x = d
            x_hat = model(x.to(device))
            diff = x - x_hat.detach().cpu()
            diff = diff.numpy()
            diff = np.power(diff, 2).mean(axis=1)
            diffs.append(diff)
    diffs = np.hstack(diffs)
    return diffs

def scaling_dataset(data):
    scaler = MinMaxScaler()
    scaler.fit(data)
    return scaler.transform(data)

def main():
    set_seed()
    args = parser.parse_args()
    data = pd.read_csv(args.data_fname, index_col=0)
    data = data.iloc[:,2:]
    data = data.to_numpy()
    
    scaler = MinMaxScaler()
    scaler.fit(data)

    trains, tests = train_test_split(data, train_size=0.9, random_state=128)
    trains, vals = train_test_split(trains, train_size=0.8, random_state=128)
#    print('train / val / test : {} / {} / {} '.format(trains.shape[0],
#        vals.shape[0],
#        tests.shape[0]))

    tests = make_anomalies(tests)
    trains = scaler.transform(trains)
    vals = scaler.transform(vals)
    tests[:, :-1] = scaler.transform(tests[:, :-1])
    
#    print('normal : {}'.format((tests[:,-1] == 0.).sum()))
#    print('outlier : {}'.format((tests[:,-1] == 1.).sum()))


    input_size = trains.shape[1]
    train_dataset = TrainDataset(data=trains)
    val_dataset = TrainDataset(data=vals)
    test_dataset = TestDataset(data=tests)
    
    batch_size = 128

    train_loader = DataLoader(dataset=train_dataset,
            batch_size=batch_size,
            shuffle=True,
            num_workers=0)
    val_loader = DataLoader(dataset=val_dataset,
            batch_size=batch_size,
            shuffle=True,
            num_workers=0)
    test_loader = DataLoader(dataset=test_dataset,
            batch_size=1,
            shuffle=False,
            num_workers=0)
    b_ratio = (tests[:, -1] == 1.).sum()/tests.shape[0]
    print('품질 성능 : {:.2f}%'.format((1-b_ratio) * 100))
    print('\n')
    print('모델 학습 ...') 
    device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
    model = train(input_size, train_loader, val_loader, device)
    val_diffs = get_diff(model, val_loader, device)
    th = val_diffs.mean() + val_diffs.std()
    
    print('\n')
    print('Before')
    print('기존 이상치 비율 : {:.2f}%'.format(b_ratio * 100))
    print('\n')
    print('데이터 보정 ... ')
    res = []
    predict = []
    model.eval()
    with torch.no_grad():
        for x, y in tqdm(test_loader):
            x_hat = model(x.to(device))
            diff = x - x_hat.detach().cpu()
            diff = diff.numpy()
            diff = np.power(diff, 2).mean(axis=1)
            if diff < th:
                res.append(y.numpy())
                predict.append(0)
            else:
                predict.append(1)
    res = np.hstack(res)
     
    print('\n')
    print('After')
    a_ratio = (res == 1.).sum()/len(res)
    print('total : ', len(res))
    print('normal : ', (res == 0.).sum())
    print('outlier : ', (res == 1.).sum())
    print('보정 후 이상치 비율 : {:.2f}%'.format(a_ratio * 100))
    res_ratio = (b_ratio - a_ratio)/b_ratio
    print('향상율 : {:.2f}%'.format(res_ratio * 100))

if __name__ == '__main__':
    main()

