import json
import argparse

from tqdm import tqdm
import numpy as np
import pandas as pd

from hanspell import spell_checker


def get_text_error_rate(input_fname, col_name):
    data = pd.read_csv(input_fname)
    
    err_lst = []
    for text in tqdm(data[col_name]):
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
    result = round(result * 100, 4)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname')
    parser.add_argument('--col_name')
    args = parser.parse_args()

    text_error_rate = get_text_error_rate(args.input_fname, args.col_name)
    print(text_error_rate, '%')
