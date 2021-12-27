
from django.shortcuts import render

from django.conf import settings
from django.contrib.sessions.backends.db import SessionStore
from django.core.files.storage import FileSystemStorage
from django.http import JsonResponse
from django.http.request import HttpRequest
from django.views.decorators.csrf import csrf_exempt

import base64
import csv
import io
import json
import math
import os
import requests
import statistics
import urllib
import urllib.request

from collections import OrderedDict
from datetime import datetime
from importlib import import_module
from urllib.parse import urlparse

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras import layers
from matplotlib import pyplot as plt
from matplotlib import rcParams
import seaborn as sns
import importlib

# global variables
g_sCurLocation = settings.MEDIA_ROOT

# view functions
def index(request):

    return render(request, 'index.html')


def test(request):
    
    # for test
    # url = "http://164.125.37.214:5555/api/fairness/tpr"
    # dictParam = {'csv': "210909085736_DATA_TRN.csv", 'tpra': "0.953", 'tprb': "0.273" }
    # headers = {"Content-Type":"application/json"}
    # try:
    #     res = requests.get(url, params=dictParam, headers = headers)
    #     res.raise_for_status()
    # except requests.exceptions.HTTPError as err:
    #     print('err:', err.response.text)

    # if res.status_code == 200:
    #     print(res.json())
    #     json_res = res.json()
    #     print(json_res['train'])
    
    return render(request, 'test_snd.html')
    

@csrf_exempt
def getkey(request):
    print('getkey::req')
    
    dictResult = {}
    dictResult['success'] = 'false'
    dictResult['key'] = None

    if request.method == 'POST':
        
        session = SessionStore()
        session.create()
        session.set_expiry(0)
        
        session['arrRcvFile'] = []
        session['objRcvFile'] = None
        session['sFileNameTrnB'] = ''
        session['sFileNameTest'] = ''
        session['sFileNameTrnA'] = ''
        session['arrColumns'] = []
        session['arrDataCsv'] = []
        session['arrDataTpr'] = []
        session['arrDataCMx'] = []
        dictStatus = {'bApiMode': 'false', 'bRunning': 'false', 'bLoad1st': 'false', 'bLoad2nd': 'false', 'bProc1st': 'false', 'bProc2nd': 'false'}
        session['dictStatus'] = dictStatus
        
        dictResult['success'] = 'true'
        dictResult['key'] = session.session_key
        session.save()

    else:
        dictResult['message'] = 'not POST method'

    print('getkey::res', dictResult)
    return JsonResponse(json.dumps(dictResult), safe = False)


def get_session(key):    
    if key == None or len(key) != 32:
        return None
    session = SessionStore(session_key = key)
    return session


@csrf_exempt
def upload(request):
    print('upload::req')
    
    global g_sCurLocation
    
    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':        
        key = request.POST.get('key')
        if key == None or key == '':
            json_data = json.loads(request.body)
            if 'key' not in json_data:
                dictResult['message'] = 'key is not set'
                print('upload::res', dictResult)    
                return JsonResponse(json.dumps(dictResult), safe = False)
            else:
                key = json_data['key']
        session = get_session(key)
        if session == None:
            dictResult['message'] = 'invalid key'        
        else:
            g_sCurLocation = settings.MEDIA_ROOT + "/csv"
            filepath = ''
            filename = ''                    

            now = datetime.now()
            stamp = now.strftime("%y%m%d%H%M%S")
            
            if request.FILES.__len__() > 0:

                rcvFile = request.FILES['file']
                
                # save copy
                filepath = stamp + '_' + rcvFile.name
                fs = FileSystemStorage(g_sCurLocation)
                filename = fs.save(filepath, rcvFile)
            
            else:                    
                url = request.POST.get('url')
                if url == None:
                    json_data = json.loads(request.body)
                    url = json_data['url']
                filename = os.path.basename( urlparse(url).path )
                filename = stamp + '_' + filename
                
                # save copy
                filepath = g_sCurLocation + '/' + filename
                urllib.request.urlretrieve(url, filepath)
                
            arrRcvFile = []
            if 'arrRcvFile' in session:
                arrRcvFile = session['arrRcvFile']
            arrRcvFile.append({ 'filename': filename, 'stamp': stamp, 'time': now.strftime("%Y-%m-%d %H:%M:%S"), 'key': session.session_key })   
            session['arrRcvFile'] = arrRcvFile
            session.save()
            print( len(arrRcvFile), filepath )
            
            dictResult['success'] = 'true'
            dictResult['filename'] = filename

    else:
        dictResult['message'] = 'not POST method'

    print('upload::res', dictResult)    
    return JsonResponse(json.dumps(dictResult), safe = False)


def getcount(request):

    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)        
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:
                nFileCnt = 0
                if 'arrRcvFile' in session:
                    arrRcvFile = session['arrRcvFile']
                    nFileCnt = len(arrRcvFile)
                    
                dictResult['success'] = 'true'
                dictResult['count'] = nFileCnt

    else:
        dictResult['message'] = 'not POST method'

    return JsonResponse(json.dumps(dictResult), safe = False)


def getlist(request):

    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)        
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:
                
                arrRcvFile = []
                if 'arrRcvFile' in session:
                    arrRcvFile = session['arrRcvFile']
                if len(arrRcvFile) > 0:
                    dictResult['success'] = 'true'
                    dictResult['list'] = arrRcvFile

    else:
        dictResult['message'] = 'not POST method'

    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def loading(request):
    print('loading::req')
    
    global g_sCurLocation
    
    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)        
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:

                if 'filename' not in json_data:
                    dictResult['message'] = 'filename is none'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                sFileNameTrn = json_data['filename']
                print(sFileNameTrn)
                
                if 'bOriginData' not in json_data:
                    dictResult['message'] = 'bOriginData not exist'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                bOriginData = json_data['bOriginData']
    
                if 'dictStatus' not in session:
                    dictResult['message'] = 'dictStatus not exist'
                    return JsonResponse(json.dumps(dictResult), safe = False)                    
                dictStatus = session['dictStatus']

                if bOriginData == True:
                    print('loading::before')

                    arrRcvFile = session['arrRcvFile']
                    objRcvFile = None

                    if  sFileNameTrn != '' and len(arrRcvFile) > 0:

                        for idx, item in enumerate(arrRcvFile):
                            if item['filename'] == sFileNameTrn:
                                objRcvFile = item
                                arrRcvFile.pop(idx)
                                break

                        session['arrRcvFile'] = arrRcvFile
                        session['objRcvFile'] = objRcvFile

                        if  objRcvFile != None and objRcvFile['filename'] != '':

                            dictStatus['bRunning'] = 'true'
                            dictStatus['bLoad1st'] = 'false'
                            dictStatus['bLoad2nd'] = 'false'
                            dictStatus['bProc1st'] = 'false'
                            dictStatus['bProc2nd'] = 'false'

                            session['sFileNameTrnB'] = objRcvFile['filename']
                            session['sFileNameTest'] = ''
                            session['sFileNameTrnA'] = ''
                            session['arrColumns'] = []
                            session['arrDataCsv'] = []
                            session['arrDataTpr'] = []
                            session['arrDataCMx'] = []
                        
                            g_sCurLocation = settings.MEDIA_ROOT + "/csv"                    
                            fs = FileSystemStorage(g_sCurLocation)
                            objFile = fs.open( objRcvFile['filename'])                
                            
                            # data set
                            csvFile = objFile.read().decode('utf8')
                            csvData = csv.reader(csvFile.splitlines())

                            nRow = 0
                            for row in csvData:
                                if nRow == 0:
                                    session['arrColumns'] = row
                                    dictData = { 'columns': session['arrColumns'], 'rows': [] }
                                else:
                                    nCol = 0
                                    dNewCol = dict.fromkeys(session['arrColumns'])
                                    for key in dNewCol.keys():
                                        dNewCol[key] = row[nCol]
                                        nCol += 1
                                    dictData['rows'].append( dNewCol )
                                nRow += 1

                            arrDataCsv = session['arrDataCsv']
                            arrDataCsv.append(dictData)
                            session['arrDataCsv'] = arrDataCsv
                            
                            dictResult['success'] = 'true'
                            if dictStatus['bApiMode'] == 'false':
                                dictResult['list'] = []
                                dictResult['list'].append( { 'data': dictData, 'name': 'before' } )
                            
                            dictStatus['bLoad1st'] = 'true'

                            session['dictStatus'] = dictStatus
                            session.save()
                        
                        else:
                            dictResult['message'] = 'file not found or filename is empty'
                            return JsonResponse(json.dumps(dictResult), safe = False)

                    else:
                        dictResult['message'] = 'filename is empty or file list is null'
                        return JsonResponse(json.dumps(dictResult), safe = False)

                else:
                    print('loading::after')

                    if  sFileNameTrn != '':

                        session['sFileNameTrnA'] = sFileNameTrn
                        
                        g_sCurLocation = settings.MEDIA_ROOT + "/result"   
                        fs = FileSystemStorage(g_sCurLocation)
                        objFile = fs.open(sFileNameTrn)

                        # after set
                        csvFile = objFile.read().decode('utf8')
                        csvData = csv.reader(csvFile.splitlines())

                        nRow = 0
                        for row in csvData:
                            if nRow == 0:
                                #session['arrColumns'] = row
                                dictData = { 'columns': session['arrColumns'], 'rows': [] }
                            else:
                                nCol = 0
                                dNewCol = dict.fromkeys(session['arrColumns'])
                                for key in dNewCol.keys():
                                    dNewCol[key] = row[nCol]
                                    nCol += 1
                                dictData['rows'].append( dNewCol )
                            nRow += 1

                        arrDataCsv = session['arrDataCsv']
                        arrDataCsv.append(dictData)
                        session['arrDataCsv'] = arrDataCsv
                        
                        dictResult['success'] = 'true'
                        if dictStatus['bApiMode'] == 'false':
                            dictResult['list'] = []
                            dictResult['list'].append( { 'data': arrDataCsv[0], 'name': 'before' } )
                            dictResult['list'].append( { 'data': arrDataCsv[1], 'name': 'after' } )
                            
                        dictStatus['bLoad2nd'] = 'true'

                        session['dictStatus'] = dictStatus
                        session.save()
    
    else:
        dictResult['message'] = 'not POST method'

    print('loading::res', dictResult['success'])    
    return JsonResponse(json.dumps(dictResult), safe = False)


def overview(request):
    print('overview::req')

    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)        
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:

                if 'arrDataCsv' not in session:
                    dictResult['message'] = 'arrDataCsv not exist'
                    print('overview::res', dictResult)    
                    return JsonResponse(json.dumps(dictResult), safe = False)
                    
                arrDataCsv = session['arrDataCsv']

                dictDataFair = {}
                dictDataFair['objRawData'] = []
                dictDataFair['arrOvColumns'] = {}
            
                for nIdxF, dataCsv in enumerate(arrDataCsv):
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

                        dictResult['success'] = 'true'
                        dictResult['data'] = dictDataFair
                                
    else:
        dictResult['message'] = 'not POST method'

    print('overview::res', dictResult['success'])    
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def indicator(request):    
    print('indicator::req')
    
    global g_sCurLocation
    
    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:
    
                if 'dictStatus' not in session:
                    dictResult['message'] = 'dictStatus not exist'
                    print('indicator::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                    
                dictStatus = session['dictStatus']
                
                if dictStatus['bLoad1st'] == 'false' and dictStatus['bLoad2nd'] == 'false':
                    dictResult['message'] = 'data not loading'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                        
                objRcvFile = session['objRcvFile']
                arrColumns = session['arrColumns']
                arrDataCsv = session['arrDataCsv']                

                CriteriaCol = json_data['CriteriaCol'] #'damage'
                CriteriaLab = json_data['CriteriaLab'] #'>1500'
                HashBuktCol = json_data['HashBuktCol'] # 'first_crash_type'
                HashBuktSiz = json_data['HashBuktSiz'] #'1000'
                
                arrCategorfeatures = json_data['Categorfeatures'] # categorical features
                arrNumericfeatures = json_data['Numericfeatures'] # numeric features

                HIDDEN_UNITS_LAYER_01 = int(json_data['Parameters'][0]) # 128 #@param
                HIDDEN_UNITS_LAYER_02 = int(json_data['Parameters'][1]) # 64 #@param
                LEARNING_RATE         = float(json_data['Parameters'][2]) # 0.1 #@param
                L1_REGULARIZATION_STRENGTH = float(json_data['Parameters'][3]) # 0.001 #@param
                L2_REGULARIZATION_STRENGTH = float(json_data['Parameters'][4]) # 0.001 #@param
                EPOCHS     = int(json_data['Parameters'][5]) # 10 #@param
                BATCH_SIZE = int(json_data['Parameters'][6]) # 500 #@param

                CATEGORY = json_data['Parameters'][7] # "crash_type" #@param
                SUBGROUPS = json_data['Parameters'][8] # "crash_type items" #@param
                CLASSES = [ json_data['Parameters'][9], json_data['Parameters'][10] ] # ['Over $1,500', 'Less than $1,500']

                RANDOM_SEED = 512
                tf.random.set_seed(RANDOM_SEED)

                fs = FileSystemStorage(g_sCurLocation)
                sFilePath = g_sCurLocation
                
                if dictStatus['bProc1st'] == 'false':
                    print('indicator::before')
                    
                    dictStatus['bProc1st'] = 'true'

                    # make test set
                    nLimit = 5000
                    nCount0 = 0
                    nCount1 = 0
                    arrSample0 = []
                    arrSample1 = []

                    for idx, row in enumerate(arrDataCsv[0]['rows']):

                        if row[CriteriaCol] == CriteriaLab:
                            if ( nCount0 < nLimit ):
                                arrSample0.append( row )
                                arrDataCsv[0]['rows'].pop(idx)
                            nCount0 += 1
                        else:
                            if ( nCount1 < nLimit ):
                                arrSample1.append( row )
                                arrDataCsv[0]['rows'].pop(idx)
                            nCount1 += 1            
                        
                        if  nCount0 >= nLimit and nCount1 >= nLimit:
                            break
                
                    arrSample = arrSample0 + arrSample1

                    sFilePath = g_sCurLocation + '/'
                    #save test set
                    sFileName = objRcvFile['stamp'] + '_DATA_TST.csv'
                    print("save_test", sFilePath, sFileName)
                    dataFrmTST = pd.DataFrame(arrSample, columns = arrColumns)
                    dataFrmTST.to_csv(sFilePath+sFileName, index = False)
                    
                    session['sFileNameTest'] = sFileName

                    # save train set
                    sFileName = objRcvFile['stamp'] + '_DATA_TRN.csv'
                    print("save_train", sFilePath, sFileName)
                    dataFrmTST = pd.DataFrame(arrDataCsv[0]['rows'], columns = arrColumns)
                    dataFrmTST.to_csv(sFilePath+sFileName, index = False)
                    
                    session['sFileNameTrnB'] = sFileName
                    
                    # load train set
                    print("load_train", sFilePath, sFileName)
                    objFile = fs.open(sFileName)
                    csvFileTRN = objFile.read().decode('utf8')

                else:
                    print('indicator::after')
                    
                    dictStatus['bProc2nd'] = 'true'

                    # load train set
                    sFilePath = g_sCurLocation + '/'
                    sFileName = session['sFileNameTrnA']
                    print("load_train", sFilePath, sFileName)
                    objFile = fs.open(sFileName)
                    csvFileTRN = objFile.read().decode('utf8')

                # load test set
                sCurLocation = settings.MEDIA_ROOT + "/csv"
                fs = FileSystemStorage(sCurLocation)
                
                sFilePath = g_sCurLocation + '/'
                sFileName = session['sFileNameTest']
                print("load_test", sFilePath, sFileName)
                objFile = fs.open(sFileName)
                csvFileTST = objFile.read().decode('utf8')

                dataFrmTRN = pd.read_csv(io.StringIO(csvFileTRN), names = arrColumns, sep=r'\s*,\s*', skiprows=[0], engine='python', na_values = "?")
                dataFrmTST = pd.read_csv(io.StringIO(csvFileTST), names = arrColumns, sep=r'\s*,\s*', skiprows=[0], engine='python', na_values = "?")

                print('train rows: ', dataFrmTRN.shape[0], ' cells: ', dataFrmTRN.shape[1])
                print('test  rows: ', dataFrmTST.shape[0], ' cells: ', dataFrmTST.shape[1])

                # set feature column
                listColumnFeatures = []

                feature_col = tf.feature_column.categorical_column_with_hash_bucket(HashBuktCol, hash_bucket_size = int(HashBuktSiz))
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
                
                listIndicatorFeatures = []
                for col in listColumnFeatures:
                    indicator = tf.feature_column.indicator_column(col)
                    listIndicatorFeatures.append(indicator)

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

                # make result
                dictResult['success'] = 'true'
                if dictStatus['bProc1st'] == 'true':
                    dictResult['filename'] = session['sFileNameTrnB']
                dictResult['Metric'] = []
                dictResult['ImgTag'] = []
                
                arrDataTpr = session['arrDataTpr']
                arrDataCMx = session['arrDataCMx']

                for subgroup in SUBGROUPS:

                    subgroup_filter  = dataFrmTST.loc[dataFrmTST[CATEGORY] == subgroup]
                    features, labels = pandas_to_numpy(subgroup_filter, CriteriaCol, CriteriaLab)
                    subgroup_results = model.evaluate(x=features, y=labels, verbose=0)

                    subgroup_performance_metrics = { 'TPR=TP/(TP+FN)': subgroup_results[7], 'FNR=FN/(TP+FN)': 1 - subgroup_results[7] }
                    performance_df = pd.DataFrame(subgroup_performance_metrics, index =[ subgroup])

                    dictResult['Metric'].append( json.loads(json.dumps(list(performance_df.T.to_dict().values()))) )

                    confusion_matrix = np.array([[subgroup_results[1], subgroup_results[4]], [subgroup_results[2], subgroup_results[3]]])
                    graph = plot_confusion_matrix(confusion_matrix, CLASSES, subgroup)

                    arrDataTpr.append(subgroup_results[7])
                    dictDataCMx = {}
                    dictDataCMx['title'] = 'Confusion Matrix for Performance Across ' + subgroup
                    dictDataCMx['axislable'] = ['Predictions', 'References']
                    dictDataCMx['ticklable'] = CLASSES
                    dictDataCMx['axisvalue'] = {
                        'True Positives': subgroup_results[1], 'False Negatives':subgroup_results[4],
                        'False Positives':subgroup_results[2], 'True Negatives': subgroup_results[3] }
                    arrDataCMx.append(dictDataCMx)

                    dictResult['ImgTag'].append( graph )

                print(arrDataTpr)
                session['arrDataTpr'] = arrDataTpr
                session['arrDataCMx'] = arrDataCMx
            
                if dictStatus['bProc2nd'] == 'true':
                    dictStatus = {'bApiMode': 'false', 'bRunning': 'false', 'bLoad1st': 'false', 'bLoad2nd': 'false', 'bProc1st': 'false', 'bProc2nd': 'false'}
                
                session['dictStatus'] = dictStatus
                session.save()

    else:
        dictResult['message'] = 'not POST method'
        
    print('indicator::res', dictResult['success'])
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def isrunning(request):
    
    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        json_data = json.loads(request.body)        
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:
                bRunning = 'false'
                if 'dictStatus' in session:
                    dictStatus = session['dictStatus']
                    bRunning = dictStatus['bRunning']
                    
                    dictResult['success'] = 'true'
                    dictResult['running'] = bRunning

    else:
        dictResult['message'] = 'not POST method'
    
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def run(request):
    print('run::req')

    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':
        json_data = json.loads(request.body)
        key = ''
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            key = json_data['key']
            session = get_session(key)
            if session == None:
                dictResult['message'] = 'invalid key'
            else:
                
                if 'filename' not in json_data:
                    dictResult['message'] = 'filename is none'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                sFileNameTrn = json_data['filename']
                print(sFileNameTrn)
                
                if 'dictStatus' in session:
                    dictStatus = session['dictStatus']
                    dictStatus['bApiMode'] = 'true'
                    session['dictStatus'] = dictStatus
                    session.save()
                
                dictParamRun = {
                    'key': key,
                    'CriteriaCol': 'damage', 'CriteriaLab': '>1500',
                    'HashBuktCol': 'first_crash_type', 'HashBuktSiz': '1000',
                    'Categorfeatures': [
                        {'ColName': 'weather_condition',
                        'VocList': ['CLEAR', 'CLOUDY/OVERCAST', 'FOG/SMOKE/HAZE', 'OTHER', 'RAIN', 'SEVERE CROSS WIND GATE', 'SLEET/HAIL', 'SNOW', 'UNKNOWN']},
                        {'ColName': 'lighting_condition',
                        'VocList': ['DARKNESS', 'DARKNESS/LIGHTED ROAD', 'DAWN', 'DAYLIGHT', 'DUSK', 'UNKNOWN']},
                        {'ColName': 'roadway_surface_cond',
                        'VocList': ['DRY', 'ICE', 'OTHER', 'SAND/MUD/DIRT', 'SNOW OR SLUSH', 'UNKNOWN', 'WET']},
                        {'ColName': 'crash_type',
                        'VocList': ['INJURY AND / OR TOW DUE TO CRASH', 'NO INJURY / DRIVE AWAY']}],
                    'Numericfeatures': [
                        {'ColName': 'posted_speed_limit',
                        'BndList': [10, 20, 30, 40, 50, 60, 70, 80]}], 
                    'Parameters': ['128', '64', '0.1', '0.001', '0.001', '10', '500', 'crash_type', ['INJURY AND / OR TOW DUE TO CRASH', 'NO INJURY / DRIVE AWAY'],
                    'Over $1,500', 'Less than $1,500']}

                #base_url =  "{0}://{1}{2}".format(request.scheme, request.get_host(), request.path)
                base_url =  "{0}://{1}/Fairness/".format(request.scheme, request.get_host())
                
                url = base_url + 'loading/'
                dictParam = {'key': key, 'filename': sFileNameTrn, 'bOriginData': True}
                res = requests.post(url, data = json.dumps(dictParam))

                if res.status_code == 200:
                    json_res = json.loads(res.json())
                    if json_res['success'] == 'true':
                        url = base_url + 'indicator/'
                        res = requests.post(url, data = json.dumps(dictParamRun))

                        if res.status_code == 200:
                            json_res = json.loads(res.json())
                            if json_res['success'] == 'true':
                                
                                aTPR = []
                                for sGrp in json_res['Metric']:
                                    for sIdx in sGrp:
                                        for sKey in sIdx:
                                            aTPR.append(sIdx[sKey])
                                
                                url = "http://164.125.37.214:5555/api/fairness/tpr"
                                #dictParam = {'csv': "210719_fairness_test_origin chicago crashes.csv", 'tpra': "0.50", 'tprb': "0.163" }
                                dictParam = {'csv': json_res['filename'], 'tpra': aTPR[0], 'tprb': aTPR[2] }
                                print(dictParam)
                                headers = {"Content-Type":"application/json"}
                                try:
                                    res = requests.get(url, params=dictParam, headers = headers)
                                    res.raise_for_status()
                                except requests.exceptions.HTTPError as err:
                                    dictResult['message'] = err.response.text
                                    print('run::res', dictResult)
                                    return JsonResponse(json.dumps(dictResult), safe = False)

                                if res.status_code == 200:
                                    json_res = res.json()
                                    print(json_res)
                                    if 'train' in json_res and json_res['train'] != '':

                                        url = base_url + 'loading/'
                                        dictParam = {'key': key, 'filename': json_res['train'], 'bOriginData': False }
                                        res = requests.post(url, data = json.dumps(dictParam))

                                        if res.status_code == 200:
                                            json_res = json.loads(res.json())
                                            if json_res['success'] == 'true':
                                                url = base_url + 'indicator/'
                                                res = requests.post(url, data = json.dumps(dictParamRun))

                                                json_res = json.loads(res.json())
                                                if json_res['success'] == 'true':

                                                    dictResult['success'] = 'true'
                                
                                # for test
                                # url = base_url + 'loading/'
                                # dictParam = {'key': key, 'filename': 'train_after.csv', 'bOriginData': False }
                                # res = requests.post(url, data = json.dumps(dictParam))

                                # if res.status_code == 200:
                                #     json_res = json.loads(res.json())
                                #     if json_res['success'] == 'true':
                                #         url = base_url + 'indicator/'
                                #         res = requests.post(url, data = json.dumps(dictParamRun))

                                #         json_res = json.loads(res.json())
                                #         if json_res['success'] == 'true':

                                #             dictResult['success'] = 'true'

    else:
        dictResult['message'] = 'not POST method'
        
    print('run::res', dictResult)
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def getresult(request):
    
    dictResult = {}
    dictResult['success'] = 'false'
    dictResult['result'] = {}

    if request.method == 'POST':            
        json_data = json.loads(request.body)
        if 'key' not in json_data:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(json_data['key'])
            if session == None:
                dictResult['message'] = 'invalid key'            
            else:
        
                arrDataTpr = session['arrDataTpr']
                arrDataCMx = session['arrDataCMx']
            
                # core = (abs(before_tpr_a-before_tpr_b)-abs(after_tpr_a-after_tpr_b))/abs(before_tpr_a-before_tpr_b)
                fValA = abs( float(arrDataTpr[0]) - float(arrDataTpr[1]) )
                fValB = abs( float(arrDataTpr[2]) - float(arrDataTpr[3]) )
                fValR = ( fValB - fValA ) / fValA
                
                dictResult['success'] = 'true'
                dictResult['result']['rate'] = fValR
                dictResult['result']['tpr'] = arrDataTpr
                dictResult['result']['cmdata'] = arrDataCMx
    else:
        dictResult['message'] = 'not POST method'
        
    return JsonResponse(json.dumps(dictResult), safe = False)

    
def pandas_to_numpy(data, colName, colValue):

    data = data.dropna(how="any", axis=0)
    labels = np.array(data[f"{colName}"] == f"{colValue}")
    features = data.drop(colName, axis=1)
    features = {name:np.array(value) for name, value in features.items()}
    
    return features, labels

    
def plot_confusion_matrix(confusion_matrix, class_names, subgroup, figsize = (8, 6)):
    
    importlib.reload(plt)
    importlib.reload(sns)

    rcParams.update({'font.family':'sans-serif', 'font.sans-serif':['Liberation Sans'],})
    plt.switch_backend('agg')

    df_cm = pd.DataFrame(confusion_matrix, index=class_names, columns=class_names,)
    
    fig = plt.figure(figsize=figsize)
    
    strings = np.asarray([['True Positives', 'False Negatives'], ['False Positives', 'True Negatives']])
    labels = (np.asarray(["{0:g}\n{1}".format(value, string) for string, value in zip(strings.flatten(), confusion_matrix.flatten())])).reshape(2, 2)

    sns.set(font_scale=1.25)
    heatmap = sns.heatmap(df_cm, annot=labels, fmt="", linewidths=2.0, cmap=sns.color_palette("GnBu_d"))
    heatmap.yaxis.set_ticklabels(heatmap.yaxis.get_ticklabels(), rotation=0, ha='right')
    heatmap.xaxis.set_ticklabels(heatmap.xaxis.get_ticklabels(), rotation=45, ha='right')
    
    plt.xlabel('Predictions')  
    plt.ylabel('References')
        
    buf = io.BytesIO()
    st = fig.suptitle('Confusion Matrix for Performance Across ' + subgroup)
    fig.savefig(buf, format='png', bbox_extra_artists=[st], bbox_inches='tight')
    buf.seek(0)
    str = base64.b64encode(buf.read())
    
    plt.close(fig)

    uri = 'data:image/png;base64,' + urllib.parse.quote(str)
    html = f'<img src = "{uri}" alt="{subgroup}" style="height:100%;width:100%;object-fit:contain"/>'


    return html
