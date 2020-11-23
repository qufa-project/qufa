# -*- coding: utf-8 -*-
import random
import argparse
import pandas as pd
import numpy as np
import datawig

parser = argparse.ArgumentParser(description='check null record in data and imputate record')
parser.add_argument('--data_fname', metavar='DIR',
        help='file name to test text quality [default: ./data/perform_info.csv]',
        default='./data/perform_info.csv')
parser.add_argument('--result_fname', metavar='DIR',
        help='file name to result correct data [default: imputer_model]',
        default='imputer_model')

def main():
    args = parser.parse_args()
    df = pd.read_csv(args.data_fname, index_col=0)
    df_train, df_test = datawig.utils.random_split(df)
#    print('train : {}'.format(df_train.shape[0]))
#    print('test : {}'.format(df_test.shape[0]))
    result = []
    for cat in df_test.category:
        r = random.uniform(0,1)
        if r >= 0.8:
            result.append(np.nan)
        else:
            result.append(cat)
    df_label = df_test.copy()
    df_test.category = result
    idx = df_test.category.isna()

    # print('Before')
    b_null_rate = df_test.category.isna().sum() / df_test.shape[0]
    print('품질 성능 : {:.2f}%'.format((1 - b_null_rate) * 100))

    print('\n')
    print('모델 학습 ... ')
    imputer = datawig.SimpleImputer(
            input_columns = ['company', 'title', 'location'],
            output_column = 'category',
            output_path = args.result_fname)
    imputer.fit(train_df = df_train)
    imputed = imputer.predict(df_test)
    
    print('Before')
    b_null_rate = df_test.category.isna().sum() / df_test.shape[0]
    print('기존 결측율 : {:.2f}%'.format(b_null_rate * 100))

    print('\n')
    print('After')
    a_null_rate = imputed.category_imputed.isna().sum()
    print('보정 결측율 : {:.2f}%'.format(a_null_rate))
    res = (b_null_rate - a_null_rate) / b_null_rate
    print('\n')
    print('향상율 : {:.2f}%'.format(res * 100))
    acc = (df_label.category[idx] == imputed.category_imputed[idx]).sum()/idx.sum()
#    print('Imputation Acc : {:.4f}'.format(acc))


if __name__ == '__main__':
    main()

