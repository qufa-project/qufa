# -*- coding: utf-8 -*-
import argparse
from tqdm import tqdm
import pandas as pd
import numpy as np
from hanspell import spell_checker

parser = argparse.ArgumentParser(description='check text quality in data')
parser.add_argument('--data_fname', metavar='DIR',
        help='file name to test text quality [default: ./data/recbook.csv]',
        default='./data/recbook.csv')
parser.add_argument('--col_name', type=str,
        help='column name to test text quality [default: description]',
        default='description')
parser.add_argument('--result_fname', metavar='DIR',
        help='file name to result correct data [default: ./data/recbook_correct.csv]',
        default='./data/recbook_correct.csv')

def check_err_rate(df, col_name):
    print('Check the Error_rate ... ')
    err_lst = []
    for text in tqdm(df[col_name]):
        try:
            result = spell_checker.check(text).as_dict()
            word_check = result['words']
            n_word = len(word_check)
            error = 0.
            if word_check:
                for k, v in word_check.items():
                    if v != 0:
                        error += 1.
            err_rate = error/n_word
            err_lst.append(err_rate)
        except:
            err_lst.append(None)
    result = np.mean([err for err in err_lst if err is not None])
    return result * 100

def correct_data(df, col_name):
    correct_lst = []
    for text in tqdm(df[col_name]):
        try:
            result = spell_checker.check(text).as_dict()
            correct_lst.append(result['checked'])
        except:
            correct_lst.append(text)
    res = df.copy()
    res[col_name] = correct_lst
    return res

def main():
    args = parser.parse_args()
    data = pd.read_csv(args.data_fname, index_col=0)
    b_ratio = check_err_rate(data, args.col_name)
    print('품질 성능 : {:.2f}%'.format((100-b_ratio)))
    print('Before')
    print('기존 텍스트 오류율 : {:.2f}%'.format(b_ratio))
    print('\n')
    print('데이터 교정 중 ... ')
    data_after = correct_data(data, args.col_name)
    print('\n')
    print('After')
    a_ratio = check_err_rate(data_after, args.col_name)
    print('보정 후 텍스트 오류율 : {:.2f}%'.format(a_ratio))
    r_ratio = (b_ratio - a_ratio)/b_ratio * 100
    print('개선 비율 : {:.2f}%'.format(r_ratio))
#    data_after.to_csv(args.result_fname)

if __name__ == '__main__':
    main()

