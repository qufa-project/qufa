import pandas as pd
import numpy as np


def LoadDataset(data, target) :
    print("LoadDataset")
    data.fillna(0, inplace=True)
    data.dropna(inplace=True)
    
    cols = list(data.columns)
    
    except_target = cols.copy()
    except_target.remove(target)
    
    return data

def get_avg(result_list) : 
    sum_0=0
    non_0=0
    for i in result_list[0] : 
        sum_0+=len(result_list[0][i])
        if len(result_list[0][i]) !=0:
            non_0+=1
    avg_0 = int(sum_0/non_0) 

    sum_1=0
    non_1=0
    for i in result_list[1] : 
        sum_1+=len(result_list[1][i])
        if len(result_list[1][i]) !=0:
            non_1+=1
    avg_1 = int(sum_1/non_1)
    
    return avg_0, avg_1

def make_result(data, target, uniq_list, group_list) : 
    result_list=[]
    for tg in data[target].unique() :
        df_list=dict()
        for tr in uniq_list :
            tmp = data[data[target]==tg].copy()
            for col, uniq in zip(group_list, tr)  : 
                tmp=tmp[tmp[col]==uniq].copy()
            df_list[tr]=tmp
        result_list.append(df_list)
    
    return result_list

def batch_append(batch, result_list, uniq_list, avg_0, avg_1) : 
    print("batch_append")
    
    batch_list=[]
    remain_result_list=[]
    
    df_list=dict()
    re_list=dict()
    for uniq in uniq_list : 
        tmp = result_list[0][uniq]
        
        if len(tmp) >= avg_0*3 :
            avg_sample=tmp.sample(avg_0*3)
        else : 
            avg_sample=tmp.copy()
        
        #avg_sample=tmp.copy()
        
        df_list[uniq]=avg_sample
        re_list[uniq]=pd.DataFrame()
        result_list[0][uniq].drop(avg_sample.index, inplace=True)
    
    batch_list.append(df_list)
    remain_result_list.append(re_list)
    
    df_list=dict()
    re_list=dict()
    for uniq in uniq_list : 
        tmp = result_list[1][uniq]
        
        if len(tmp) >= avg_1*3 :
            avg_sample=tmp.sample(avg_1*3)
        else : 
            avg_sample=tmp.copy()
            
        #avg_sample=tmp.copy()
        
        df_list[uniq]=avg_sample
        re_list[uniq]=pd.DataFrame()
        result_list[1][uniq].drop(avg_sample.index, inplace=True)
    
    batch_list.append(df_list)
    remain_result_list.append(re_list)
    
    remain=pd.DataFrame()
    for uniq in uniq_list : 
        remain=pd.concat([remain,result_list[0][uniq]])
        remain=pd.concat([remain,result_list[1][uniq]])
        
    remain=pd.concat([remain, batch])
    
    return batch_list, remain, remain_result_list

def make_fair(data, target, except_target, batch) : 
    
    group_list = np.array(except_target)
    
    uniq_list=data[except_target].groupby(list(group_list)).count()
    uniq_list = dict(zip(uniq_list.index.values, uniq_list.values))
    print("make_fair")

    result_list = make_result(data, target, uniq_list, group_list)
    avg_0, avg_1 = get_avg(result_list) 
        
    batch_list, remain, remain_result_list = batch_append(batch, result_list, uniq_list, avg_0, avg_1)
    
    
    remain_uniq_list=remain[except_target].groupby(list(group_list)).count()
    remain_uniq_list = dict(zip(remain_uniq_list.index.values, remain_uniq_list.values))
    
    for tg in remain[target].unique() :
        df_list=dict()
        for tr in remain_uniq_list :
            tmp = remain[remain[target]==tg].copy()
            for col, uniq in zip(group_list, tr)  : 
                tmp=tmp[tmp[col]==uniq].copy()
            df_list[tr]=tmp
            remain_result_list[tg][tr] = tmp
    
    test_list = remain_uniq_list
    for i in uniq_list : 
        test_list.pop(i, [])
        
    for i in test_list : 
        batch_list[0][i] = remain_result_list[0][i].copy()
        batch_list[1][i] = remain_result_list[1][i].copy()
        
        remain_result_list[0][i].drop(batch_list[0][i].index, inplace=True)
        remain_result_list[1][i].drop(batch_list[1][i].index, inplace=True)
    
    uniq_list.update(remain_uniq_list)
    
    print('Add dif')
    for i in uniq_list : 
        if len(batch_list[0][i]) > len(batch_list[1][i]) : 
            dif = len(batch_list[0][i]) - len(batch_list[1][i])
            if len(remain_result_list[1][i]) > dif : 
                tmp = remain_result_list[1][i].sample(dif)
                batch_list[1][i] = pd.concat([batch_list[1][i],tmp])
                remain_result_list[1][i].drop(tmp.index, inplace=True)
            else :
                batch_list[1][i] = pd.concat([batch_list[1][i],remain_result_list[1][i]])
                remain_result_list[1][i].drop(remain_result_list[1][i].index, inplace=True)
            
        if len(batch_list[1][i]) > len(batch_list[0][i]) : 
            dif = len(batch_list[1][i]) - len(batch_list[0][i])
            if len(remain_result_list[0][i]) > dif : 
                tmp = remain_result_list[0][i].sample(dif)
                batch_list[0][i] = pd.concat([batch_list[0][i],tmp])
                remain_result_list[0][i].drop(tmp.index, inplace=True)
            else :
                batch_list[0][i] = pd.concat([batch_list[0][i],remain_result_list[0][i]])
                remain_result_list[0][i].drop(remain_result_list[0][i].index, inplace=True)

    min_size=len(data)
    
    for i in result_list :
        for j in i :
            if len(i[j]) > 0 :
                if len(i[j]) < min_size : 
                    min_size=len(i[j])
    
    min_list = []
    for i in result_list :
        for j in i :
            min_tmp = []
            if len(i[j]) > min_size :
                min_tmp.append(j)
        min_tmp = set(min_tmp)
        min_list.append(min_tmp)
                            
    print("min_size : "+str(min_size))
    if min_size > 0 :            
        for i in range(len(remain_result_list)) :
            for j in min_list[i] : 
                tmp = remain_result_list[i][j].sample(min_size)
                remain_result_list[i][j].drop(tmp.index, inplace=True)
                batch_list[i][j] = pd.concat([batch_list[i][j], tmp])
                

    train=pd.DataFrame()
    for i in uniq_list : 
        train=pd.concat([train,batch_list[0][i], batch_list[1][i]])

            
    remain=pd.DataFrame()
    for i in uniq_list : 
        remain=pd.concat([remain,remain_result_list[0][i], remain_result_list[1][i]])     
            
    return train, remain