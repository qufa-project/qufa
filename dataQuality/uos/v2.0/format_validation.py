import re
import argparse

import pandas as pd


date_regex = re.compile(r"^(20[0-9][0-9])-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])")
time_regex = re.compile(r"^(0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])")
phone_regex = re.compile(r"(\d{2}-|\d{3}-)?(\d{3}|\d{4})-(\d{4})")
number_regex = re.compile(r"\d+")


def get_error_rate(series, error_idxs):
    total_count = len(series)
    error_count = len(error_idxs)
    error_rate = error_count / total_count * 100
    print(round(error_rate, 4), '%')


def date_check(date_series):
    error_idxs = list()
    for i, d in date_series.items():
        if not date_regex.match(d):
            error_idxs.append(i)
    return error_idxs


def time_check(time_series):
    error_idxs = list()
    for i, t in time_series.items():
        if not time_regex.match(t):
            error_idxs.append(i)
    return error_idxs


def phone_check(phone_series):
    error_idxs = list()
    for i, p in phone_series.items():
        if not phone_regex.match(p):
            error_idxs.append(i)
    return error_idxs


def date_correct(df, col):
    error_idxs = date_check(df[col])
    error_dates = df.loc[error_idxs, col].tolist()
    clean_dates = list()
    for error_date in error_dates:
        numbers = number_regex.findall(error_date)
        clean_dates.append('-'.join(numbers))
    df.loc[error_idxs, col] = clean_dates
    return df


def time_correct(df, col):
    error_idxs = time_check(df[col])
    error_times = df.loc[error_idxs, col].tolist()
    clean_times = list()
    for error_time in error_times:
        numbers = number_regex.findall(error_time)
        clean_times.append(':'.join(numbers))
    df.loc[error_idxs, col] = clean_times
    return df


def phone_correct(df, col):
    error_idxs = phone_check(df[col])
    error_phones = df.loc[error_idxs, col].tolist()
    clean_phones = list()
    for error_phone in error_phones:
        numbers = number_regex.findall(error_phone)
        clean_phones.append('-'.join(numbers))
    df.loc[error_idxs, col] = clean_phones
    return df


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname', default='./data/event.csv')
    parser.add_argument('--result_fname', default='event-after.csv')
    parser.add_argument('--func_name')
    parser.add_argument('--col_name')
    args = parser.parse_args()
    
    df = pd.read_csv(args.input_fname)
    format_type, perform_type = args.func_name.split('_')

    if perform_type == 'check':
        series = df[args.col_name]
        if format_type == 'date':
            error_idxs = date_check(series)
        elif format_type == 'time':
            error_idxs = time_check(series)
        elif format_type == 'phone':
            error_idxs = phone_check(series)
        else:
            raise ValueError
        get_error_rate(series, error_idxs)

    elif perform_type == 'correct':
        if format_type == 'date':
            df_clean = date_correct(df, args.col_name)
        elif format_type == 'time':
            df_clean = time_correct(df, args.col_name)
        elif format_type == 'phone':
            df_clean = phone_correct(df, args.col_name)
        else:
            raise ValueError
        df_clean.to_csv(args.result_fname, index=False)
    else:
        raise ValueError

