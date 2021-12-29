from sklearn.svm import LinearSVC
import numpy as np



def get_info(y_test, y_hat) : 
    tp = np.sum((y_test ==1) & (y_hat==1) )
    tn = np.sum((y_test ==0) & (y_hat==0) )
    fp = np.sum((y_test ==0) & (y_hat==1) )
    fn = np.sum((y_test ==1) & (y_hat==0) )
    
    accuracy = np.mean(np.equal(y_test,y_hat))
    
    return tp, tn, fp, fn, accuracy

def f_val(train, test, target, subgroup) : 
    cols = list(train.columns)
    except_target = cols.copy()
    except_target.remove(target)

    y_train=train[target].astype(int)
    X_train=train[except_target].astype(int)

    CATEGORY  =  subgroup
    SUBGROUP = 0 
    X_test_a  = test.loc[test[CATEGORY] == SUBGROUP][except_target]
    y_test_a  = test.loc[test[CATEGORY] == SUBGROUP][target]

    SUBGROUP = 1 
    X_test_b  = test.loc[test[CATEGORY] == SUBGROUP][except_target]
    y_test_b  = test.loc[test[CATEGORY] == SUBGROUP][target]

    model = LinearSVC()
    model.fit(X_train, y_train)

    y_hat = model.predict(X_test_a)
    tp, tn, fp, fn, accuracy_a = get_info(y_test_a, y_hat)
    before_tpr_a = tp/(tp+fn)
    before_fpr_a = fp/(fp+tn)
    before_dp_a = (tn+fn)/(tp+fp)


    y_hat = model.predict(X_test_b)
    tp, tn, fp, fn, accuracy_b = get_info(y_test_b, y_hat)
    before_tpr_b = tp/(tp+fn)
    before_fpr_b = fp/(fp+tn)
    before_dp_b = (tn+fn)/(tp+fp)


    print("before TPRA : " + str(before_tpr_a) + "/ before FPRA : " + str(before_fpr_a) + 
    "/ before DFA : " + str(before_dp_a)+ "/ before DFA : " + str(accuracy_a))
    print("before TPRB : " + str(before_tpr_b) + "/ before FPRB : " + str(before_fpr_b) + 
    "/ before DFB : " + str(before_dp_b)+ "/ before DFA : " + str(accuracy_b))

    return before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b


def val(train, test, target, subgroup, before_tpr_a, before_tpr_b, before_fpr_a, before_fpr_b, before_dp_a, before_dp_b) : 
    cols = list(train.columns)
    except_target = cols.copy()
    except_target.remove(target)

    y_fair=train[target].astype(int)
    X_fair=train[except_target].astype(int)

    CATEGORY  =  subgroup
    SUBGROUP = 0 
    X_test_a  = test.loc[test[CATEGORY] == SUBGROUP][except_target]
    y_test_a  = test.loc[test[CATEGORY] == SUBGROUP][target]

    SUBGROUP = 1 
    X_test_b  = test.loc[test[CATEGORY] == SUBGROUP][except_target]
    y_test_b  = test.loc[test[CATEGORY] == SUBGROUP][target]

    model = LinearSVC()
    model.fit(X_fair, y_fair)

    y_hat = model.predict(X_test_a)
    tp, tn, fp, fn, accuracy = get_info(y_test_a, y_hat)
    after_tpr_a = tp/(tp+fn)
    after_fpr_a = fp/(fp+tn)
    after_dp_a = (tn+fn)/(tp+fp)

    y_hat = model.predict(X_test_b)
    tp, tn, fp, fn, accuracy = get_info(y_test_b, y_hat)
    after_tpr_b = tp/(tp+fn)
    after_fpr_b = fp/(fp+tn)
    after_dp_b = (tn+fn)/(tp+fp)

    print("after TPRA : " + str(after_tpr_a) + "/ after FPRA : " + str(after_fpr_a) + "/ after DFA : " + str(after_dp_a))
    print("after TPRB : " + str(after_tpr_b) + "/ after FPRB : " + str(after_fpr_b) + "/ after DFB : " + str(after_dp_b))

    score_TPR = (abs(before_tpr_a-before_tpr_b)-abs(after_tpr_a-after_tpr_b))/abs(before_tpr_a-before_tpr_b)
    print("Equality of Opportunity : " + str(score_TPR))

    score_TPRFPR = ((abs(before_tpr_a-before_tpr_b)-abs(after_tpr_a-after_tpr_b))/abs(before_tpr_a-before_tpr_b) + 
                    (abs(before_fpr_a-before_fpr_b)-abs(after_fpr_a-after_fpr_b))/abs(before_fpr_a-before_fpr_b))/2
    print("Equalized odds : " + str(score_TPRFPR))

    score_DP = (abs(before_dp_a-before_dp_b)-abs(after_dp_a-after_dp_b))/abs(before_dp_a-before_dp_b)
    print("Demographic Parity : " + str(score_DP))

    print("Accuracy : " + str(accuracy))

    avg_score = (score_TPR+score_TPRFPR+score_DP)/3
    print("Score : " + str(avg_score))
    print("--------------------------------------------------------")

    return after_tpr_a, after_tpr_b, after_fpr_a, after_fpr_b, after_dp_a, after_dp_b