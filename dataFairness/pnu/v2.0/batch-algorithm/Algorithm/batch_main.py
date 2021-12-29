import batch_fair_func as fair
import batch_val_func as val
import pandas as pd
import warnings
warnings.filterwarnings(action='ignore')




def main(train, batch, num, target) : 

    data = fair.LoadDataset(batch, target)

    if num==0 :
        train = data.copy()
        remain=pd.DataFrame()

        cols = list(train.columns)
        except_target = cols.copy()
        except_target.remove(target)

    else :
        
        cols = list(train.columns)

        except_target = cols.copy()
        except_target.remove(target)

        train, remain = fair.make_fair(train, target, except_target, data)

    num+=1
    
    return train, remain, num



data_0 = pd.read_csv("../data/input/first_data.csv")
data_1 = pd.read_csv("../data/input/second_data.csv")
data_2 = pd.read_csv("../data/input/third_data.csv")
data_3 = pd.read_csv("../data/input/fourth_data.csv")
data_4 = pd.read_csv("../data/input/fifth_data.csv")
data_5 = pd.read_csv("../data/input/sixth_data.csv")
test_data = pd.read_csv("../data/input/test_data.csv")

data_list = [data_0, data_1, data_2, data_3, data_4, data_5]

num=0
target ="heart_risk10"
subgroup = "sex"

train=pd.DataFrame()
remain = pd.DataFrame()

test = test_data
for i in range(6) : 
    remain = pd.concat([remain, data_list[i]])
    train, remain, num = main(train, remain, num, target)
    train.to_csv("../data/output/result_batch"+str(i+1)+".csv", index=False)
    remain.to_csv("../data/output/remain_batch"+str(i+1)+".csv", index=False)
    print(len(train), len(remain))
    if num == 1 : 
        before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b = val.f_val(train,test,target,subgroup)
        first_tpr_a, first_tpr_b, first_fpr_a, first_fpr_b, first_dp_a, first_dp_b = before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b
    else :
        before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b = val.val(train,test,target,subgroup, before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b)
        
val.val(train,test,target,subgroup, first_tpr_a, first_tpr_b, first_fpr_a, first_fpr_b, first_dp_a, first_dp_b)