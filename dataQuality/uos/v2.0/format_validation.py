import pandas as pd
import datetime 
import re
import numpy as np
import argparse


def day_check(date_text):
  err_rate = 0

  for d in date_text :
    try :
      datetime.datetime.strptime(str(d), "%Y-%m-%d")
    except ValueError:
      if(d != '0') :  
        err_rate=err_rate+1
     
  print(round(err_rate/len(date_text) * 100,4),"%")

def day_correct(date_text):
  c=0
  for d in date_text :
    try :
      datetime.datetime.strptime(str(d), "%Y-%m-%d")
    except ValueError:
      if(d != '0') :  
        d = re.sub(r"[-=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]","",str(d))
        d = datetime.datetime.strptime(d,"%Y%m%d")
        d = datetime.date.strftime(d,"%Y-%m-%d")
    
    date_text[c] = d   
    c+=1


def time_check(time_text):
  err_rate = 0

  for d in time_text :
    try :
      datetime.datetime.strptime(str(d), "%H:%M")
    except ValueError:
      if(d != '0') :
        err_rate = err_rate+1  
      
  print(round(err_rate/len(time_text) * 100,4),"%")

def time_correct(time_text) :
  c=0
  passData = ['0','상세 일정 참조','미정','프로그램별 상이','월','화','수','목','금']
  for d in time_text :
    try :
      datetime.datetime.strptime(str(d), "%H:%M")
    except ValueError:
      if(d != '0') :
        if(d not in passData) :  
          if any(len(d) > 5 and i in d for i in '/~-,'):
            d = d.replace('/','~').replace('-','~').replace(',','~')
            dA = d.split('~')
            time_text[c] = dA[0]
          else : 
            d = re.sub(r"[-=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]","",str(d))
            if(d.isdigit()) :
              d = datetime.datetime.strptime(d.zfill(4),"%H%M")
              d = datetime.date.strftime(d,"%H:%M")
    time_text[c] = d    
    c+=1


def phone_check(phone_text):
  err_rate = 0

  errA = "[=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]"
  for d in phone_text :
    if(d != '0') :  
      if any(i in d for i in errA):
        err_rate = err_rate+1
  print(round(err_rate/len(phone_text) * 100,4),"%")


def phone_correct(phone_text):
  c=0

  errA = "[=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]"
  for d in phone_text :
    if(d != '0') :  
      d = re.sub(r"[=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]","",str(d))
      chk = re.findall(r"[\d]{3}-[\d]{3}-[\d]{3}", d)
    
    phone_text[c] = d
    c+=1


def formatValidation(input_fname, result_fname, func_name, col_name):
  df = pd.read_csv(input_fname)
  df.fillna('0', inplace=True)

  if(func_name=="dayCheck") :
    day_check(df[col_name])
  elif (func_name=="timeCheck") :
    time_check(df[col_name])
  elif (func_name=="phoneCheck") :
    phone_check(df[col_name])
  elif(func_name=="dayCorrect") :
    day_correct(df[col_name])
    df.to_csv(result_fname)
  elif (func_name=="timeCorrect") :
    time_correct(df[col_name])
    df.to_csv(result_fname)
  elif (func_name=="phoneCorrect") :
    phone_correct(df[col_name])
    df.to_csv(result_fname)
  else :
    print("입력 값이 잘못되었습니다.")
 
 
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_fname',default='./data/event.csv')
    parser.add_argument('--result_fname', default='event-after.csv')
    parser.add_argument('--func_name')
    parser.add_argument('--col_name')
    args = parser.parse_args()

    formatValidation(input_fname=args.input_fname, result_fname=args.result_fname, func_name=args.func_name, col_name=args.col_name)
