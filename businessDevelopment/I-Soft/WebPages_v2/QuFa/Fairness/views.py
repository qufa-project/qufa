from django.shortcuts import render

from django.conf import settings
from django.http import JsonResponse
from django.core.files.storage import FileSystemStorage

import os
import io
import math
import statistics
import base64
import urllib
import csv
import json
from collections import OrderedDict

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras import layers
from matplotlib import pyplot as plt
from matplotlib import rcParams
import seaborn as sns


# Create your views here.

g_arrColumns = []
g_arrDataCsv = []
g_arrFileCsv = []


def index(request):

    os.environ['TF_XLA_FLAGS'] = '--tf_xla_enable_xla_devices'
    physical_devices = tf.config.list_physical_devices('GPU') 
    tf.config.experimental.set_memory_growth(physical_devices[0], True)

    return render(request, 'index.html')


def upload(request):
    
    if request.method == 'POST':

        global g_arrColumns
        global g_arrDataCsv
        global g_arrFileCsv

        dictResult = {}
        dictResult['success'] = 'false'
        dictResult['message'] = 'fail'

        if request.FILES.__len__() == 0:
            dictResult['message'] = "업로드할 파일이 없습니다."
            return JsonResponse(json.dumps(dictResult))

        uploadFile0 = request.FILES['file0']
        if uploadFile0.name.find('csv') < 0 :
            dictResult['message'] = "파일형식이 잘못되었습니다"
            return JsonResponse(json.dumps(dictResult))

        uploadFile1 = request.FILES['file1']
        if uploadFile1.name.find('csv') < 0 :
            dictResult['message'] = "파일형식이 잘못되었습니다"
            return JsonResponse(json.dumps(dictResult))

        g_arrColumns = json.loads( request.POST.get('columns[]') )
        g_arrDataCsv = []
        
        dictResult['success'] = 'true'
        dictResult['message'] = 'success'
        dictResult['list'] = []

        # 01
        csvFile = uploadFile0.read().decode('utf8')
        g_arrFileCsv.append(csvFile)
        csvData = csv.reader(csvFile.splitlines())

        dictData = { 'columns': g_arrColumns, 'rows': [] }
        for row in csvData:

            nCol = 0
            dNewCol = dict.fromkeys(g_arrColumns)
            for key in dNewCol.keys():
                dNewCol[key] = row[nCol]
                nCol += 1

            dictData['rows'].append( dNewCol )

        g_arrDataCsv.append(dictData)
        dictResult['list'].append( { 'data': dictData, 'name': uploadFile0.name } )
        
        # 02
        csvFile = uploadFile1.read().decode('utf8')
        g_arrFileCsv.append(csvFile)
        csvData = csv.reader(csvFile.splitlines())

        nRow = 0
        dictData = { 'columns': g_arrColumns, 'rows': [] }
        for row in csvData:

            nCol = 0
            dNewCol = dict.fromkeys(g_arrColumns)
            for key in dNewCol.keys():
                dNewCol[key] = row[nCol]
                nCol += 1

            dictData['rows'].append( dNewCol )
            nRow += 1

        g_arrDataCsv.append(dictData)
        dictResult['list'].append( { 'data': dictData, 'name': uploadFile1.name } )


        # filename_src = settings.MEDIA_ROOT + '\\DATA_TRN.csv'
        # with open(filename_src, 'w', encoding='UTF8', newline='') as f:
        #     writer = csv.DictWriter(f, fieldnames=g_arrDataCsv[0]['columns'])
        #     writer.writeheader()
        #     writer.writerows(g_arrDataCsv[0]['rows'])
            
        # print(filename_src)

        # fs = FileSystemStorage(location=settings.MEDIA_ROOT)
        # filename = fs.open(filename_src)
        # file_url = fs.url(filename)
        
        # filename = fs.save(filename_src, uploadFile0)
        # file_url = fs.url(filename)

        # print(filename, " ", file_url)
        
        return JsonResponse(json.dumps(dictResult), safe = False)


def overview(request):

    if request.method == 'POST':

        global g_arrDataCsv

        dictDataFair = {}
        dictDataFair['objRawData'] = []
        dictDataFair['arrOvColumns'] = {}
    
        for nIdxF, dataCsv in enumerate(g_arrDataCsv):
            dictDataFair['objRawData'].append(dataCsv)
            
            # Make Overview
            for sKey in dataCsv['columns']:
                if sKey not in dictDataFair['arrOvColumns']:
                    dictDataFair['arrOvColumns'][sKey] = {}
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'] = {}
                    
                if nIdxF not in dictDataFair['arrOvColumns'][sKey]['arrOvData']:
                    dictOvCol = {}
                    dictOvCol['bNumeric'] = False
                    dictOvCol['nCount'] = 0
                    dictOvCol['nError'] = 0
                    dictOvCol['fSum'] = 0
                    dictOvCol['fMean'] = 0
                    dictOvCol['fStdDev'] = 0
                    dictOvCol['nZeros'] = 0
                    dictOvCol['fMin'] = float('inf')
                    dictOvCol['fMedian'] = 0
                    dictOvCol['fMax'] = 0
                    dictOvCol['sMaxUniqueKey'] = ''
                    dictOvCol['pnUniqueCntr'] = {}
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF] = dictOvCol

                for nRow, aRow in enumerate(dataCsv['rows']):                    
                    item = aRow[sKey]

                    if nRow == 0:
                        dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['bNumeric'] = item.isdigit()
                    
                    fVal = 0
                    if dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['bNumeric']:
                        fVal = float(item)
                        if np.isnan(fVal):
                            dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['nError'] += 1
                            continue
                    else:
                        fVal = len(item)
                        if fVal == 0:                        
                            dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['nError'] += 1
                            continue

                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['nCount'] += 1
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fSum'] += fVal
                    fPrevMin = dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMin']
                    fPrevMax = dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMax']
                    if fPrevMin > fVal: dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMin'] = fVal
                    if fPrevMax < fVal: dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMax'] = fVal
                    if fVal == 0: dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['nZeros'] += 1
                    if item not in dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr']:
                        dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr'][item] = 0
                        
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr'][item] += 1

                dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr'] = OrderedDict(sorted(dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr'].items()))

                nRowCnt = dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['nCount']
                dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMean'] = dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fSum'] / nRowCnt

                if dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['bNumeric']:
                    SDprep = 0
                    for nRow, aRow in enumerate(dataCsv['rows']):                        
                        fVal = float(aRow[sKey])
                        SDprep += math.pow((fVal - dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMean']), 2)
                        
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fStdDev'] = math.sqrt(SDprep/nRowCnt)

                    arrNumber = []
                    for sVal in dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr']:                    
                        fVal = float(sVal)
                        arrNumber.append(fVal)

                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['fMedian'] = statistics.median(arrNumber)
                    
                else:                    
                    nMax = 0
                    sMaxKey = ''
                    for sVal in dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr']:                    
                        nLen = int(dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['pnUniqueCntr'][sVal])
                        if nMax < nLen:
                            nMax = nLen
                            sMaxKey = sVal                        
                    dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['sMaxUniqueKey'] = sMaxKey
                    
        #print(dictDataFair['arrOvColumns'])

        return JsonResponse(json.dumps(dictDataFair), safe = False)


def indicator(request):
    
    if request.is_ajax() and request.method == 'POST':
        
        
        json_data = json.loads(request.body)
        #print(json_data)

        CriteriaCol = json_data['CriteriaCol'] #'damage'
        CriteriaLab = json_data['CriteriaLab'] #'>1500'
        HashBuktCol = json_data['HashBuktCol'] # 'first_crash_type'
        HashBuktSiz = json_data['HashBuktSiz'] #'1000'

        HIDDEN_UNITS_LAYER_01 = int(json_data['Parameters'][0]) # 128 #@param
        HIDDEN_UNITS_LAYER_02 = int(json_data['Parameters'][1]) # 64 #@param
        LEARNING_RATE         = float(json_data['Parameters'][2]) # 0.1 #@param
        L1_REGULARIZATION_STRENGTH = float(json_data['Parameters'][3]) # 0.001 #@param
        L2_REGULARIZATION_STRENGTH = float(json_data['Parameters'][4]) # 0.001 #@param
        EPOCHS     = int(json_data['Parameters'][5]) # 10 #@param
        BATCH_SIZE = int(json_data['Parameters'][6]) # 500 #@param
        CATEGORY = json_data['Parameters'][7] # "weather_condition" #@param {type:"string"}
        SUBGROUP = json_data['Parameters'][8] # "RAIN" #@param {type:"string"}
        CLASSES = [ json_data['Parameters'][9], json_data['Parameters'][10] ] # ['Over $1,500', 'Less than $1,500']

        RANDOM_SEED = 512
        tf.random.set_seed(RANDOM_SEED)

        global g_arrColumns
        global g_arrFileCsv

        # fileCsvTRN = tf.keras.utils.get_file('crashes.data.trn', 'http://164.125.37.222:18099/data/201008/201008_origin_122219624.csv')
        # fileCsvTST = tf.keras.utils.get_file('crashes.data.tst', 'http://164.125.37.222:18099/data/201008/201008_testset.csv')

        # dataFrmTRN = pd.read_csv(fileCsvTRN, names=g_arrColumns, sep=r'\s*,\s*',  engine='python', na_values="?")
        # dataFrmTST = pd.read_csv(fileCsvTST, names=g_arrColumns, sep=r'\s*,\s*', skiprows=[0], engine='python', na_values="?")
        
        dataFrmTRN = pd.read_csv(io.StringIO(g_arrFileCsv[0]), names = g_arrColumns, sep=r'\s*,\s*', engine='python', na_values = "?")
        dataFrmTST = pd.read_csv(io.StringIO(g_arrFileCsv[1]), names = g_arrColumns, sep=r'\s*,\s*', engine='python', na_values = "?")

        print('train rows: ', dataFrmTRN.shape[0], ' cells: ', dataFrmTRN.shape[1])
        print('test  rows: ', dataFrmTST.shape[0], ' cells: ', dataFrmTST.shape[1])

        listColumnFeatures = []

        feature_col = tf.feature_column.categorical_column_with_hash_bucket(HashBuktCol, hash_bucket_size = int(HashBuktSiz))
        listColumnFeatures.append(feature_col)
        
        for row in json_data['Categorfeatures']:
            arrVocList = []
            for item in row['VocList']:
                arrVocList.append(f"{item}")
            #print(f"{row['ColName']} - {arrVocList}")
            feature_col = tf.feature_column.categorical_column_with_vocabulary_list(row['ColName'], arrVocList)
            listColumnFeatures.append(feature_col)
        
        for row in json_data['Numericfeatures']:
            arrBndList = []
            for item in row['BndList']:
                arrBndList.append(int(item))
            #print(f"{row['ColName']} - {arrBndList}")
            numeric_col = tf.feature_column.numeric_column(row['ColName'])
            feature_col = tf.feature_column.bucketized_column(numeric_col, boundaries = arrBndList)
            listColumnFeatures.append(feature_col)

        
        # first_crash_type = tf.feature_column.categorical_column_with_hash_bucket("first_crash_type", hash_bucket_size=1000)
        # weather_condition = tf.feature_column.categorical_column_with_vocabulary_list(
        #     "weather_condition", ["CLEAR", "RAIN"])
        # lighting_condition = tf.feature_column.categorical_column_with_vocabulary_list(
        #     "lighting_condition", ["DARKNESS", "DARKNESS LIGHTED ROAD", "DAWN", "DAYLIGHT", "DUSK", "UNKNOWN"])
        # roadway_surface_cond = tf.feature_column.categorical_column_with_vocabulary_list(
        #     "roadway_surface_cond", ["DRY", "ICE", "OTHER", "SAND/MUD/DIRT", "SNOW OR SLUSH", "UNKNOWN", "WET"])
        # posted_speed_limit = tf.feature_column.numeric_column("posted_speed_limit")
        # posted_speed_limit_buckets = tf.feature_column.bucketized_column(posted_speed_limit, boundaries=[10, 20, 30, 40, 50, 60, 70, 80])
            

        listIndicatorFeatures = []
        for col in listColumnFeatures:
            indicator = tf.feature_column.indicator_column(col)
            listIndicatorFeatures.append(indicator)
        # listIndicatorFeatures.append(tf.feature_column.indicator_column(posted_speed_limit_buckets))
        # listIndicatorFeatures.append(tf.feature_column.indicator_column(weather_condition))
        # listIndicatorFeatures.append(tf.feature_column.indicator_column(first_crash_type))
        # listIndicatorFeatures.append(tf.feature_column.indicator_column(lighting_condition))
        # listIndicatorFeatures.append(tf.feature_column.indicator_column(roadway_surface_cond))

        #print(listIndicatorFeatures)

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
            layers.DenseFeatures(listIndicatorFeatures),
            layers.Dense(HIDDEN_UNITS_LAYER_01, activation='relu', kernel_regularizer=regularizer),
            layers.Dense(HIDDEN_UNITS_LAYER_02, activation='relu', kernel_regularizer=regularizer),
            layers.Dense(1, activation='sigmoid', kernel_regularizer=regularizer)
        ])

        model.compile(optimizer=tf.keras.optimizers.Adagrad(LEARNING_RATE), loss=tf.keras.losses.BinaryCrossentropy(), metrics=METRICS)

        features, labels = pandas_to_numpy(dataFrmTRN, CriteriaCol, CriteriaLab)
        model.fit(x=features, y=labels, epochs=EPOCHS, batch_size=BATCH_SIZE)

        features, labels = pandas_to_numpy(dataFrmTST, CriteriaCol, CriteriaLab)
        model.evaluate(x=features, y=labels)

        subgroup_filter  = dataFrmTST.loc[dataFrmTST[CATEGORY] == SUBGROUP]
        features, labels = pandas_to_numpy(subgroup_filter, CriteriaCol, CriteriaLab)
        subgroup_results = model.evaluate(x=features, y=labels, verbose=0)
        confusion_matrix = np.array([[subgroup_results[1], subgroup_results[4]], [subgroup_results[2], subgroup_results[3]]])

        print(subgroup_results)

        subgroup_performance_metrics = { 'TPR=TP/(TP+FN)': subgroup_results[7], 'FNR=FN/(TP+FN)': 1 - subgroup_results[7] }
        performance_df = pd.DataFrame(subgroup_performance_metrics, index=[SUBGROUP])
        graph = plot_confusion_matrix(confusion_matrix, CLASSES, SUBGROUP)
        
        dictResult = {}
        dictResult['Metric'] = json.loads(json.dumps(list(performance_df.T.to_dict().values())))
        dictResult['ImgTag'] = graph

        return JsonResponse(json.dumps(dictResult), safe = False)

    
def pandas_to_numpy(data, colName, colValue):

    data = data.dropna(how="any", axis=0)
    labels = np.array(data[f"{colName}"] == f"{colValue}")
    features = data.drop(colName, axis=1)
    features = {name:np.array(value) for name, value in features.items()}
    
    return features, labels

    
def plot_confusion_matrix(confusion_matrix, class_names, subgroup, figsize = (8, 6)):
    
    df_cm = pd.DataFrame(confusion_matrix, index=class_names, columns=class_names,)

    rcParams.update({'font.family':'sans-serif', 'font.sans-serif':['Liberation Sans'],})

    plt.switch_backend('agg')
    
    fig = plt.figure(figsize=figsize)
    
    plt.title('Confusion Matrix for Performance Across ' + subgroup)

    strings = np.asarray([['True Positives', 'False Negatives'], ['False Positives', 'True Negatives']])
    labels = (np.asarray(["{0:g}\n{1}".format(value, string) for string, value in zip(strings.flatten(), confusion_matrix.flatten())])).reshape(2, 2)

    sns.set_context("notebook", font_scale=1.25)
    heatmap = sns.heatmap(df_cm, annot=labels, fmt="", linewidths=2.0, cmap=sns.color_palette("GnBu_d"))
    heatmap.yaxis.set_ticklabels(heatmap.yaxis.get_ticklabels(), rotation=0, ha='right')
    heatmap.xaxis.set_ticklabels(heatmap.xaxis.get_ticklabels(), rotation=45, ha='right')
    
    plt.xlabel('Predictions')  
    plt.ylabel('References')  

    plt.tight_layout()
    buf = io.BytesIO()
    fig.savefig(buf, format='png')
    buf.seek(0)
    str = base64.b64encode(buf.read())

    uri = 'data:image/png;base64,' + urllib.parse.quote(str)
    html = '<img src = "%s"/>' % uri

    return html
