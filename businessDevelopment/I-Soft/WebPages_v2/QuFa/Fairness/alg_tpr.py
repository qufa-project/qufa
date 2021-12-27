
import base64
import importlib
import io
import urllib

import pandas as pd
import numpy as np

from matplotlib import pyplot as plt
from matplotlib import rcParams
import seaborn as sns

import tensorflow as tf
from tensorflow.keras import layers

from sklearn.linear_model import LogisticRegression, LinearRegression, SGDClassifier
from sklearn.svm import LinearSVC
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import classification_report

from sklearn.manifold import TSNE
import umap.umap_ as um


def run_gi(dictParams, dataFrmTRN, dataFrmTST):
    print('gi::start')              
 
    # parameters
    ClassifyCol = dictParams['ClassifyCol'] # 'damage'
    ClassifyVals = dictParams['ClassifyVals'] # ['>1500', '<=1500]
    ClassifyLbls = dictParams['ClassifyLbls'] # ['Over $1,500', 'Less than $1,500']
    SubGroupCol = dictParams['SubGroupCol'] # 'crash_type'
    SubGroupVals = dictParams['SubGroupVals'] # "crash_type items" #@param

    HashBucketSize = int(dictParams['AlgParameters']['HashBucketSize']) #'1000'
    HIDDEN_UNITS_LAYER_01 = int(dictParams['AlgParameters']['Parameters'][0]) # 128 #@param
    HIDDEN_UNITS_LAYER_02 = int(dictParams['AlgParameters']['Parameters'][1]) # 64 #@param
    LEARNING_RATE         = float(dictParams['AlgParameters']['Parameters'][2]) # 0.1 #@param
    L1_REGULARIZATION_STRENGTH = float(dictParams['AlgParameters']['Parameters'][3]) # 0.001 #@param
    L2_REGULARIZATION_STRENGTH = float(dictParams['AlgParameters']['Parameters'][4]) # 0.001 #@param
    EPOCHS     = int(dictParams['AlgParameters']['Parameters'][5]) # 10 #@param
    BATCH_SIZE = int(dictParams['AlgParameters']['Parameters'][6]) # 500 #@param
    
    arrCategorfeatures = dictParams['AlgParameters']['Categorfeatures'] # categorical features
    arrNumericfeatures = dictParams['AlgParameters']['Numericfeatures'] # numeric features

    RANDOM_SEED = 512
    tf.random.set_seed(RANDOM_SEED)

    # set feature column
    listColumnFeatures = []

    feature_col = tf.feature_column.categorical_column_with_hash_bucket(SubGroupCol, hash_bucket_size = HashBucketSize)
    listColumnFeatures.append(feature_col)
    
    for row in arrCategorfeatures:
        arrVocList = []
        for item in row['VocList']:
            arrVocList.append(f"{item}")
        #print(f"{row['ColName']} - {arrVocList}")
        feature_col = tf.feature_column.categorical_column_with_vocabulary_list(row['ColName'], arrVocList)
        listColumnFeatures.append(feature_col)
    
    for row in arrNumericfeatures:
        arrBndList = []
        for item in row['BndList']:
            arrBndList.append(int(item))
        #print(f"{row['ColName']} - {arrBndList}")
        numeric_col = tf.feature_column.numeric_column(row['ColName'])
        feature_col = tf.feature_column.bucketized_column(numeric_col, boundaries = arrBndList)
        listColumnFeatures.append(feature_col)
    
    listDenseFeatures = []
    for col in listColumnFeatures:
        indicatorCol = tf.feature_column.indicator_column(col)
        listDenseFeatures.append(indicatorCol)

    # set param & run
    METRICS = [
        tf.keras.metrics.TruePositives(name='tp'),
        tf.keras.metrics.FalsePositives(name='fp'),
        tf.keras.metrics.TrueNegatives(name='tn'),
        tf.keras.metrics.FalseNegatives(name='fn'), 
        tf.keras.metrics.BinaryAccuracy(name='accuracy'),
        tf.keras.metrics.Precision(name='precision'),
        tf.keras.metrics.Recall(name='recall'),
        tf.keras.metrics.AUC(name='auc'),
    ]

    regularizer = tf.keras.regularizers.l1_l2(l1=L1_REGULARIZATION_STRENGTH, l2=L2_REGULARIZATION_STRENGTH)

    model = tf.keras.Sequential([
        layers.DenseFeatures(listDenseFeatures),
        layers.Dense(HIDDEN_UNITS_LAYER_01, activation='relu', kernel_regularizer=regularizer),
        layers.Dense(HIDDEN_UNITS_LAYER_02, activation='relu', kernel_regularizer=regularizer),
        layers.Dense(1, activation='sigmoid', kernel_regularizer=regularizer)
    ])

    model.compile(optimizer=tf.keras.optimizers.Adagrad(LEARNING_RATE), loss=tf.keras.losses.BinaryCrossentropy(), metrics=METRICS)

    features, labels = pandas_to_numpy(dataFrmTRN, ClassifyCol, ClassifyVals[0])
    model.fit(x=features, y=labels, epochs=EPOCHS, batch_size=BATCH_SIZE)

    features, labels = pandas_to_numpy(dataFrmTST, ClassifyCol, ClassifyVals[0])
    model.evaluate(x=features, y=labels)

    # make result
    arrConfMatrix = []
    arrHMapElemnt = []
    arrHtmlImgTag = []
    
    for subgroup in SubGroupVals:

        subgroup_filter  = dataFrmTST.loc[dataFrmTST[SubGroupCol] == subgroup]
        features, labels = pandas_to_numpy(subgroup_filter, ClassifyCol, ClassifyVals[0])
        subgroup_results = model.evaluate(x=features, y=labels, verbose=0)
        
        TP = subgroup_results[1]
        FP = subgroup_results[2]
        TN = subgroup_results[3]
        FN = subgroup_results[4]

        performance_metrics = {
            'TP': TP,
            'TN': TN,
            'FP': FP,
            'FN': FN,
            'TPR': TP/(TP+FN),
            'TNR': TN/(TN+FP),
            'FPR': FP/(FP+TN),
            'FNR': FN/(TP+FN)
        }
        arrConfMatrix.append(performance_metrics)

        dictDataCMx = {}
        dictDataCMx['title'] = 'Confusion Matrix for Performance Across ' + subgroup
        dictDataCMx['axislable'] = ['Predictions', 'References']
        dictDataCMx['ticklable'] = ClassifyLbls
        dictDataCMx['axisvalue'] = { 'True Positives': TP, 'False Negatives': FN, 'False Positives': FP, 'True Negatives': TN }
        arrHMapElemnt.append(dictDataCMx)

        confusion_matrix = np.array([[TP, FN], [FP, TN]])
        graph = plot_confusion_matrix(confusion_matrix, ClassifyLbls, subgroup)
        arrHtmlImgTag.append( graph )

    print('gi::end', arrConfMatrix)

    return arrConfMatrix, arrHMapElemnt, arrHtmlImgTag


def run_sklearn(dictParams, dataFrmTRN, dataFrmTST):
    print('skl::start')

    AlgorithmType = 'svm'
    if 'AlgorithmType' in dictParams:
        AlgorithmType = dictParams['AlgorithmType'] # lr, sdg, svm
        
    print('skl::alg', AlgorithmType)

    ClassifyCol = dictParams['ClassifyCol'] # 'damage'
    ClassifyVals = []
    for item in dictParams['ClassifyVals']:  # ['>1500', '<=1500]
        if item.isdecimal():
             ClassifyVals.append( int(item) )
        else:
             ClassifyVals.append( f"{item}" )
    ClassifyLbls = dictParams['ClassifyLbls'] # ['Over $1,500', 'Less than $1,500']

    SubGroupCol = dictParams['SubGroupCol'] # 'crash_type'
    SubGroupVals = []
    for item in dictParams['SubGroupVals']:  # 'crash_type items'
        if item.isdecimal():
             SubGroupVals.append( int(item) )
        else:
             SubGroupVals.append( f"{item}" )

    RunTsneUmap = 'off'
    if 'RunTsneUmap' in dictParams:
        RunTsneUmap = dictParams['RunTsneUmap']

    print(ClassifyCol)
    print(ClassifyVals)
    print(ClassifyLbls)
    print(SubGroupCol)
    print(SubGroupVals)
    
    # set param & run model
    FeaturesCol = list(dataFrmTRN.columns)
    FeaturesCol.remove(ClassifyCol)

    dfFeaturesTrn = dataFrmTRN[FeaturesCol].copy() # X
    dfClassifyTrn = dataFrmTRN[ClassifyCol].copy() # y
        
    # dictLE = {}
    # le = LabelEncoder()
    # dfClassifyTrn = le.fit_transform(dfClassifyTrn)
    # dictLE[ClassifyCol] = le

    # for col in dfFeaturesTrn.columns:
    #     le = LabelEncoder()
    #     dfFeaturesTrn.loc[:,col] = le.fit_transform(dfFeaturesTrn[col].copy())
    #     dictLE[col] = le
    
    # # prevent unseen labels error
    # for col in dataFrmTRN.columns:
    #     le = dictLE[col]
    #     for label in np.unique(dataFrmTST[col]):
    #         if label not in le.classes_:
    #             le.classes_ = np.append(le.classes_, label)
    
    # for col in dataFrmTRN.columns:
    #     le = dictLE[col]
    #     dataFrmTST.loc[:,col] = le.transform(dataFrmTST[col].copy())
    #     dataFrmTRN.loc[:,col] = le.transform(dataFrmTRN[col].copy())

    # sc = MinMaxScaler()
    # sc.fit(dfFeaturesTrn)
    # dfFeaturesTrn = sc.transform(dfFeaturesTrn)

    model = None
    if AlgorithmType == 'lr':
        model = LogisticRegression()
    elif AlgorithmType == 'sdg':
        model = SGDClassifier()
    else: # svm
        model = LinearSVC(C = 1.0)
    model.fit(dfFeaturesTrn, dfClassifyTrn)

    # make result
    arrConfMatrix = []
    arrHMapElemnt = []
    arrHtmlImgTag = []

    # index = dictLE[SubGroupCol].transform(SubGroupVals)
    # lable = dictLE[SubGroupCol].inverse_transform(index)
    # subgroups = []
    # for idx, item in enumerate(index):
    #     subgroups.append( {'index': item, 'label': lable[idx]} )

    # for sub in subgroups:

    #     subfeatures = dataFrmTST.loc[dataFrmTST[SubGroupCol] == sub['index']][FeaturesCol].copy()
    #     sublables   = dataFrmTST.loc[dataFrmTST[SubGroupCol] == sub['index']][ClassifyCol].copy()
    for sub in SubGroupVals:

        subfeatures = dataFrmTST.loc[dataFrmTST[SubGroupCol] == sub][FeaturesCol].copy()
        sublables   = dataFrmTST.loc[dataFrmTST[SubGroupCol] == sub][ClassifyCol].copy()

        print(dataFrmTST.head())

        results = model.predict(subfeatures)
        print(classification_report(sublables, results))
        
        #Accuracy = np.mean(np.equal(sublables, results))
        TP = np.sum((sublables == 1) & (results == 1))
        TN = np.sum((sublables == 0) & (results == 0))
        FP = np.sum((sublables == 0) & (results == 1))
        FN = np.sum((sublables == 1) & (results == 0))

        performance_metrics = {
            'TP': str(TP),
            'TN': str(TN),
            'FP': str(FP),
            'FN': str(FN),
            #'ACCURACY': Accuracy,
            'TPR': str(TP/(TP+FN)),
            'TNR': str(TN/(TN+FP)),
            'FPR': str(FP/(FP+TN)),
            'FNR': str(FN/(TP+FN))
        }
        arrConfMatrix.append(performance_metrics)

        dictDataCMx = {}
        dictDataCMx['title'] = 'Confusion Matrix for ' + f"{SubGroupCol} {sub}" # f"{sub['label']
        dictDataCMx['axislable'] = ['Predictions', 'References']
        dictDataCMx['ticklable'] = ClassifyLbls
        dictDataCMx['axisvalue'] = {
            'True Positives': str(TP), 'False Negatives':str(FN),
            'False Positives':str(FP), 'True Negatives': str(TN) }
        arrHMapElemnt.append(dictDataCMx)

        confusion_matrix = np.array([[TP, FN], [FP, TN]])
        graph = plot_confusion_matrix(confusion_matrix, ClassifyLbls, dictDataCMx['title'])
        arrHtmlImgTag.append( graph )

    # t-SNE & UMAP
    if RunTsneUmap == 'on':
        tsne = TSNE(n_components=2).fit_transform(dfFeaturesTrn)
        umap = um.UMAP().fit_transform(dfFeaturesTrn)

        dfTsneUmap = dataFrmTRN.copy()
        dfTsneUmap["tsne_dim_1"] = tsne[:, 0]
        dfTsneUmap["tsne_dim_2"] = tsne[:, 1]
        dfTsneUmap["umap_dim_1"] = umap[:, 0]
        dfTsneUmap["umap_dim_2"] = umap[:, 1]

        print(dfTsneUmap.head())

        graph = plot_tsne(dfTsneUmap, ClassifyCol, ClassifyLbls)
        arrHtmlImgTag.append( graph )

    print('skl::end', arrConfMatrix)

    return arrConfMatrix, arrHMapElemnt, arrHtmlImgTag

    
def pandas_to_numpy(data, colName, colValue):

    data = data.dropna(how="any", axis=0)
    labels = np.array(data[f"{colName}"] == f"{colValue}")
    features = data.drop(colName, axis=1)
    features = {name:np.array(value) for name, value in features.items()}
    
    return features, labels

    
def plot_confusion_matrix(confusion_matrix, class_names, title):
    
    importlib.reload(plt)
    importlib.reload(sns)

    rcParams.update({'font.family':'sans-serif', 'font.sans-serif':['Liberation Sans'],})
    plt.switch_backend('agg')

    df_cm = pd.DataFrame(confusion_matrix, index=class_names, columns=class_names,)
    
    fig = plt.figure(figsize=(8,6))
    
    strings = np.asarray([['True Positives', 'False Negatives'], ['False Positives', 'True Negatives']])
    labels = (np.asarray(["{0:g}\n{1}".format(value, string) for string, value in zip(strings.flatten(), confusion_matrix.flatten())])).reshape(2, 2)

    sns.set(font_scale=1.25)
    heatmap = sns.heatmap(df_cm, annot=labels, fmt="", linewidths=2.0, cmap=sns.color_palette("GnBu_d"))
    heatmap.yaxis.set_ticklabels(heatmap.yaxis.get_ticklabels(), rotation=0, ha='right')
    heatmap.xaxis.set_ticklabels(heatmap.xaxis.get_ticklabels(), rotation=45, ha='right')
    
    plt.xlabel('Predictions')  
    plt.ylabel('References')
        
    buf = io.BytesIO()
    st = fig.suptitle(title)
    fig.savefig(buf, format='png', bbox_extra_artists=[st], bbox_inches='tight')
    buf.seek(0)
    str = base64.b64encode(buf.read())
    
    plt.close(fig)

    uri = 'data:image/png;base64,' + urllib.parse.quote(str)
    html = f'<img src = "{uri}" style="height:100%;width:100%;object-fit:contain"/>'

    return html


def plot_tsne(df, title, labels):
    
    importlib.reload(plt)
    importlib.reload(sns)
    
    plt.switch_backend('agg')
    
    fig = plt.figure(figsize=(8,16))

    colors = ["#005A9C", "#FFA500"] # "dodgerblue" "orange"
    sns.set_palette(sns.color_palette(colors))

    ax1 = fig.add_subplot(2,1,1)
    ax1.set_title("t-SNE")
    sp1 = sns.scatterplot(data=df, x="tsne_dim_1", y="tsne_dim_2", hue=title, s=10, ax=ax1)
    # sp1.legend(loc='upper right', title=title, labels=labels)

    ax2 = fig.add_subplot(2,1,2)
    ax2.set_title("UMAP")
    sp2 = sns.scatterplot(data=df, x="umap_dim_1", y="umap_dim_2", hue=title, s=10, ax=ax2)
    # sp2.legend(loc='upper right', title=title, labels=labels)
    
    buf = io.BytesIO()
    st = fig.suptitle('t-SNE & UMAP')
    fig.savefig(buf, format='png', bbox_extra_artists=[st], bbox_inches='tight')
    buf.seek(0)
    str = base64.b64encode(buf.read())
    
    plt.close(fig)

    uri = 'data:image/png;base64,' + urllib.parse.quote(str)
    html = f'<img src = "{uri}" style="height:100%;width:100%;object-fit:contain"/>'

    return html
