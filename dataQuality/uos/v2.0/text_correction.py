import json
import argparse

from tqdm import tqdm
import numpy as np
import pandas as pd

from hanspell import spell_checker


def correct_text(input_fname, result_fname, col_name):
    data = pd.read_csv(input_fname)
    
    correct_lst = []
    for text in tqdm(data[col_name]):
        try:
            result = spell_checker.check(text).as_dict()
            correct_lst.append(result['checked'])
        except:
            correct_lst.append(text)
    res = data.copy()
    res[col_name] = correct_lst
    res.to_csv(result_fname, index=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname')
    parser.add_argument('--result_fname')
    parser.add_argument('--col_name')
    args = parser.parse_args()

    correct_text(args.input_fname, args.result_fname, args.col_name)
