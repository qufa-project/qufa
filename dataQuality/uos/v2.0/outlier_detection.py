import os
import json
import random
import argparse

from tqdm import tqdm
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
import torch.nn.init as weight_init
from torch.utils.data import Dataset, DataLoader


DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')


def set_seed(seed):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


class OutlierDataset(Dataset):
    
    def __init__(self, X):
        self.X = X.astype('float')

    def __getitem__(self, index):
        x = self.X[index, :]
        x = torch.tensor(x, dtype=torch.float32)
        return index, x

    def __len__(self):
        return len(self.X)


class Model(nn.Module):
    def __init__(self, input_size, dropout=0.5):
        super(Model, self).__init__()
        self.dropout = dropout
        if self.dropout > 0:
            self.dropout = nn.Dropout(dropout)
        
        self.encode_w1 = nn.Linear(input_size, 64)
        self.encode_w2 = nn.Linear(64, 32)
        self.decode_w1 = nn.Linear(32, 64)
        self.decode_w2 = nn.Linear(64, input_size)
    
    def encoder(self, x):
        x = self.encode_w1(x)
        x = torch.relu(x)
        x = self.encode_w2(x)
        x = torch.relu(x)
        if self.dropout:
            x = self.dropout(x)
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


class Detector(object):

    def __init__(
            self,
            lr=3e-3,
            weight_decay=1e-5,
            batch_size=128,
            epochs=10
            ):
        self.lr = lr
        self.weight_decay = weight_decay
        self.batch_size = batch_size
        self.epochs = epochs
        self.threshold = 0.5
    
    def cal_recon_err(self, preds, targets):
        recon_err = F.mse_loss(preds, targets, reduction='none').mean(axis=-1)
        return recon_err

    def cal_loss(self, preds, targets):
        loss_mse = self.cal_recon_err(preds, targets)
        return loss_mse.mean()
    
    def run_batch(self, batch, train):
        idx, x = batch
        inputs = x.to(DEVICE)
        outputs = self.model(inputs)
        if train:
            self.optimizer.zero_grad()
            train_err = self.cal_recon_err(outputs, inputs)
            loss = train_err.mean()
            loss.backward()
            self.optimizer.step()
        else:
            loss = self.cal_loss(outputs, inputs)
        loss = loss.item()
        bsz = inputs.size(0)
        return loss * bsz, bsz, train_err.detach().cpu().tolist()

    def train(self, epoch=None):
        self.model.train()
        total_loss = 0
        total_cnt = 0
        train_errs = []

        for batch_idx, batch in enumerate(self.train_iter):
            
            loss, bsz, train_err = self.run_batch(batch, train=True)
            total_loss += loss
            total_cnt += bsz
            train_errs += train_err

        status = {'total_loss':total_loss/total_cnt}
        mean = np.mean(train_errs)
        std = np.std(train_errs)
        self.threshold = mean + 2*std

        return status

    def get_model(self, input_size):
        self.model = Model(input_size=input_size).to(DEVICE)
        self.optimizer = optim.Adam(self.model.parameters(),
                lr=self.lr,
                weight_decay=self.weight_decay)

    def fit(self, X):
        
        dataset = OutlierDataset(X)
        self.train_iter = DataLoader(dataset=dataset,
                batch_size=self.batch_size,
                shuffle=True)

        self.get_model(X.shape[1])

        wait = 0
        best_loss = 1e9
        iteration = tqdm(range(1, self.epochs + 1))
        
        for epoch in iteration:
            epoch_status = self.train(epoch)
            
            if best_loss > epoch_status['total_loss']:
                best_loss = epoch_status['total_loss']
                wait = 0
            else:
                wait += 1

            if wait > 3:
                break

        return self

    def extract(self, X):
        dataset = OutlierDataset(X)
        outlier_iter = DataLoader(dataset=dataset,
                batch_size=self.batch_size)
        
        outlier_idxs = []
        self.model.eval()
        with torch.no_grad():
            for batch in outlier_iter:
                idx, x = batch
                inputs = x.to(DEVICE)
                outputs = self.model(inputs)
                recon_err = self.cal_recon_err(outputs, inputs)

                outlier_idx = recon_err > self.threshold
                outlier_idx = idx[outlier_idx]

                outlier_idxs += outlier_idx.tolist()

        return outlier_idxs

    def fit_extract(self, X, **fit_params):

        return self.fit(X, **fit_params).extract(X)


class OutlierDetector(object):

    def __init__(self, input_fname, train_fname, result_path):
        self.get_data(input_fname, train_fname)
        self.input_fname = input_fname
        self.result_path = result_path

    def get_data(self, input_fname, train_fname):
        data = pd.read_csv(input_fname)
        num_idx = data.dtypes[data.dtypes != 'object'].index
        num_vars = [data.columns.get_loc(idx) for idx in num_idx]
        cat_vars = list(set(range(data.shape[1])) - set(num_vars))

        trainset = pd.read_csv(train_fname)
        
        self.data = data
        self.trainset = trainset
        self.num_vars = num_vars
        self.cat_vars = cat_vars

    def write_json(self, outlier_idxs):
        obj = {"result": dict()}
        obj["result"]["num_outliers"] = len(outlier_idxs)
        obj["result"]["outlier_indices"] = outlier_idxs
        
        result_json_fname = os.path.join(self.result_path, "result.json")
        with open(result_json_fname, "w") as json_file:
            json.dump(obj, json_file)

    def run(self):
    
        if not os.path.isdir(self.result_path):
            os.makedirs(self.result_path)
        
        X_train = self.trainset.iloc[:, self.num_vars]
        scaler = StandardScaler().fit(X_train)
        X_train = scaler.transform(X_train)

        X_noise = self.data.iloc[:, self.num_vars]
        X_noise = scaler.transform(X_noise)

        detector = Detector()
        detector.fit(X_train)
        outlier_idxs = detector.extract(X_noise)
        self.write_json(outlier_idxs)

        n = self.data.shape[0]
        idxs = list(range(n))
        clear_idxs = list(set(idxs) - set(outlier_idxs))
        result_csv_fname = '{}.csv'.format(self.result_path)
        self.data.iloc[clear_idxs, :].to_csv(result_csv_fname, index=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname', type=str, default='./data/box_office.csv')
    parser.add_argument('--train_fname', type=str, default='./data/box_office_clean.csv')
    parser.add_argument('--result_path', type=str, default='box_office-after')
    args = parser.parse_args()

    detector = OutlierDetector(input_fname=args.input_fname,
            train_fname=args.train_fname,
            result_path=args.result_path)
    detector.run()
