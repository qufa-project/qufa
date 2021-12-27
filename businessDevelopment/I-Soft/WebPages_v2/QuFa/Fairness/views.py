
from django.shortcuts import render

from django.conf import settings
from django.contrib.sessions.backends.db import SessionStore
from django.core.files.storage import FileSystemStorage
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

from Fairness import alg_tpr

import copy
import csv
import io
import json
import math
import os
import requests
import statistics
import time
import urllib.request

from collections import OrderedDict
from datetime import datetime
from threading import Thread
from urllib.parse import urlparse

import numpy as np
import pandas as pd

from pandas.api.types import is_string_dtype
from pandas.api.types import is_numeric_dtype

# global variables
g_dictStatus = {'bRunning': 'false', 'bLoad1st': 'false', 'bLoad2nd': 'false', 'bProc1st': 'false', 'bProc2nd': 'false'}
g_dictAlgParam = {
    'ClassifyCol': 'damage',
    'ClassifyVals': ['<=1500', '>1500'],
    'ClassifyLbls': ['Less than $1,500', 'Over $1,500'],
    'SubGroupCol': 'crash_type',
    'SubGroupVals': ['INJURY AND / OR TOW DUE TO CRASH', 'NO INJURY / DRIVE AWAY'],
    'AlgorithmType': 'gi',
    'AlgParameters' : {
        'HashBucketSize': '1000',
        'Parameters': ['128', '64', '0.1', '0.001', '0.001', '10', '500'],
        'Categorfeatures': [
            {'ColName': 'weather_condition',
            'VocList': ['CLEAR', 'CLOUDY/OVERCAST', 'FOG/SMOKE/HAZE', 'OTHER', 'RAIN', 'SEVERE CROSS WIND GATE', 'SLEET/HAIL', 'SNOW', 'UNKNOWN']},
            {'ColName': 'lighting_condition',
            'VocList': ['DARKNESS', 'DARKNESS/LIGHTED ROAD', 'DAWN', 'DAYLIGHT', 'DUSK', 'UNKNOWN']},
            {'ColName': 'roadway_surface_cond',
            'VocList': ['DRY', 'ICE', 'OTHER', 'SAND/MUD/DIRT', 'SNOW OR SLUSH', 'UNKNOWN', 'WET']},
            {'ColName': 'first_crash_type',
            'VocList': ['ANGLE', 'ANINAL', 'FIXED OBJECT', 'HEAD ON', 'OTHER NONCOLLISION', 'OVERTURNED', 'PARKED MOTOR VEHICLE',
                'PEDELCYCLIST', 'PEDESTRAIN', 'REAR END', 'SIDESWIPE OPPOSITE DIRECTION', 'SIDEWIPE SAME DIRECTION', 'TRAIN', 'TURNING']}
        ],
        'Numericfeatures': [{'ColName': 'posted_speed_limit', 'BndList': [10, 20, 30, 40, 50, 60, 70, 80]}]
    },
    'RunTsneUmap': 'off'
}

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

    global g_dictStatus
    global g_dictAlgParam
    
    dictResult = {}
    dictResult['success'] = 'false'
    dictResult['key'] = None

    if request.method == 'POST':
        
        session = SessionStore()
        session.create()
        session.set_expiry(0)
        
        session['bApiMode'] = 'false'
        
        session['arrRcvFile'] = []
        session['objRcvFile'] = None
        session['sFileNameTrnB'] = ''
        session['sFileNameTrnA'] = ''
        session['arrColumns'] = []
        session['arrDataCsv'] = []
        session['arrDataSnB'] = []
        session['dictAlgParam'] = g_dictAlgParam
        session['arrConfMatrix'] = []
        session['arrHMapElemnt'] = []
        session['arrHtmlImgTag'] = []
        session['dictStatus'] = g_dictStatus
        
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
            session['sCurLocation'] = settings.MEDIA_ROOT + "csv/"
            filepath = ''
            filename = ''                    

            now = datetime.now()
            stamp = now.strftime("%y%m%d%H%M%S")
            
            if request.FILES.__len__() > 0:

                rcvFile = request.FILES['file']
                
                # save copy
                filepath = stamp + '_' + rcvFile.name
                fs = FileSystemStorage(session['sCurLocation'])
                filename = fs.save(filepath, rcvFile)
            
            else:                    
                url = request.POST.get('url')
                if url == None:
                    json_data = json.loads(request.body)
                    url = json_data['url']
                filename = os.path.basename( urlparse(url).path )
                filename = stamp + '_' + filename
                
                # save copy
                filepath = session['sCurLocation'] + filename
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
                            session['sFileNameTrnA'] = ''
                            session['arrColumns'] = []
                            session['arrDataCsv'] = []
                            session['arrDataSnB'] = []
                            session['arrConfMatrix'] = []
                            session['arrHMapElemnt'] = []
                            session['arrHtmlImgTag'] = []
                        
                            session['sCurLocation'] = settings.MEDIA_ROOT + "csv/"
                            fs = FileSystemStorage(session['sCurLocation'])
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
                            if session['bApiMode'] == 'false':
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
                        
                        session['sCurLocation'] = settings.MEDIA_ROOT + "result/"
                        fs = FileSystemStorage(session['sCurLocation'])
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
                        if session['bApiMode'] == 'false':
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
                                dictDataFair['arrOvColumns'][sKey]['arrOvData'][nIdxF]['bNumeric'] = item.isdecimal()
                            
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
def sunburst(request):
    print('sunburst::req')

    global g_dictAlgParam

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
                    return JsonResponse(json.dumps(dictResult), safe = False)
                dictStatus = session['dictStatus']
                
                if dictStatus['bLoad1st'] == 'false' and dictStatus['bLoad2nd'] == 'false':
                    dictResult['message'] = 'data not loading'
                    return JsonResponse(json.dumps(dictResult), safe = False)

                if 'arrDataCsv' not in session:
                    dictResult['message'] = 'arrDataCsv not exist'
                    print('sunburst::res', dictResult)    
                    return JsonResponse(json.dumps(dictResult), safe = False)
                
                if 'dictAlgParam' not in session:
                    dictResult['message'] = 'dictAlgParam not exist'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                dictAlgParam = session['dictAlgParam']
                    
                arrDataCsv = session['arrDataCsv']
                arrDataSnB = session['arrDataSnB'] = []

                arrProcCsv = []
                if session['bApiMode'] == 'true':
                    # parameters
                    ClassifyCol = dictAlgParam['ClassifyCol'] # 'damage'
                    SubGroupCol = dictAlgParam['SubGroupCol'] # 'crash_type'
                    # process data
                    arrProcCsv = arrDataCsv
                else:
                    # parameters
                    ClassifyCol = json_data['ClassifyCol'] # 'damage'
                    SubGroupCol = json_data['SubGroupCol'] # 'crash_type'
                    # process data
                    if dictStatus['bLoad2nd'] == 'true':
                        arrProcCsv.append(arrDataCsv[1])
                    else:
                        arrProcCsv.append(arrDataCsv[0])
                        
                
                st = time.time()

                for dataCsv in arrProcCsv:
                    dataFrm = pd.DataFrame(dataCsv['rows'], columns = dataCsv['columns'])

                    aRemainCols = list(dataFrm.columns)
                    aRemainCols.remove(ClassifyCol)
                    aRemainCols.remove(SubGroupCol)

                    dictData = { 'name': 'root', 'children': [] }
                    
                    for sUnqM in np.sort(dataFrm[ClassifyCol].unique()):
                        dictItem0 = { 'name': sUnqM, 'children': [] }
                        
                        dfSub0 = dataFrm.loc[dataFrm[ClassifyCol]==sUnqM][dataFrm.columns]

                        for sUnqS in np.sort(dfSub0[SubGroupCol].unique()):
                            dictItem1 = { 'name': sUnqS, 'children': [] }

                            dfSub1 = dfSub0.loc[dfSub0[SubGroupCol]==sUnqS][dfSub0.columns]

                            arrCols = copy.deepcopy(aRemainCols)
                            dictItem1['children'] = getChildren(dfSub1, arrCols)

                            dictItem0['children'].append(dictItem1)

                        dictData['children'].append(dictItem0)

                    arrDataSnB.append(dictData)
                    
                print("sunburst::ProcTime {} sec".format(time.time()-st))

                dictResult['success'] = 'true'
                dictResult['data'] = session['arrDataSnB'] = arrDataSnB
                session.save()
                
    else:
        dictResult['message'] = 'not POST method'

    print('sunburst::res', dictResult['success'])    
    return JsonResponse(json.dumps(dictResult), safe = False)

def getChildren(dataFrm, arrCols):   

    arrChildren = []

    if ( len(arrCols) <= 0 ):
        print(len(arrCols))
        return arrChildren
    elif ( len(arrCols) > 1 ):
        sCol = arrCols[0]
        arrCols.remove(sCol)
        for sUnq in np.sort(dataFrm[sCol].unique()):
            dictItem = { 'name': sUnq, 'children': [] }
        
            dfSub = dataFrm.loc[dataFrm[sCol]==sUnq][dataFrm.columns]
            arrSubCols = copy.deepcopy(arrCols)
            dictItem['children'] = getChildren(dfSub, arrSubCols)
            arrChildren.append(dictItem)
    else:
        sCol = arrCols[0]
        arrCols.remove(sCol)
        for sUnq in np.sort(dataFrm[sCol].unique()):

            dfUnq = dataFrm.loc[dataFrm[sCol]==sUnq][sCol]
            sVal = str(len(dfUnq.tolist()))
            arrChildren.append({'name': f"{sUnq} ({sVal})", 'value': sVal})

    return arrChildren

@csrf_exempt
def run_alg(request):
    print('run_alg::req')
        
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
                    print('run_alg::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                dictStatus = session['dictStatus']
                
                if dictStatus['bLoad1st'] == 'false' and dictStatus['bLoad2nd'] == 'false':
                    dictResult['message'] = 'data not loading'
                    print('run_alg::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                
                if 'dictAlgParam' not in session:
                    dictResult['message'] = 'dictAlgParam not exist'
                    print('run_alg::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                dictAlgParam = session['dictAlgParam']
                        
                objRcvFile = session['objRcvFile']
                arrColumns = session['arrColumns']
                arrDataCsv = copy.deepcopy(session['arrDataCsv'])
                # parameters
                ClassifyCol  = dictAlgParam['ClassifyCol']  = json_data['ClassifyCol']  # 'damage'
                ClassifyVals = dictAlgParam['ClassifyVals'] = json_data['ClassifyVals'] # ['>1500','<=1500']
                ClassifyLbls = dictAlgParam['ClassifyLbls'] = json_data['ClassifyLbls'] # ['Over $1,500', 'Less than $1,500']
                SubGroupCol  = dictAlgParam['SubGroupCol']  = json_data['SubGroupCol']  # 'crash_type'
                SubGroupVals = dictAlgParam['SubGroupVals'] = json_data['SubGroupVals'] # 'crash_type items'
                AlgorithmType= dictAlgParam['AlgorithmType']= json_data['AlgorithmType']# 'gi'
                AlgParameters= dictAlgParam['AlgParameters']= {} # for 'gi'
                if 'AlgParameters' in json_data:
                    AlgParameters = dictAlgParam['AlgParameters'] = json_data['AlgParameters']
                RunTsneUmap  = dictAlgParam['RunTsneUmap']  = json_data['RunTsneUmap']  # 'off'
                TestsetPath  = dictAlgParam['TestsetPath']  = json_data['TestsetPath']  # 'testset path'
                TestsetFile  = dictAlgParam['TestsetFile']  = json_data['TestsetFile']  # 'testset file'
                session['dictAlgParam'] = dictAlgParam

                fs = FileSystemStorage(session['sCurLocation'])
                sFilePath = session['sCurLocation']
                
                if dictStatus['bProc1st'] == 'false':
                    print('run_alg::before')
                    
                    dictStatus['bProc1st'] = 'true'

                    # load train set
                    sFileName = session['sFileNameTrnB']
                    print("load_train", sFilePath, sFileName)
                    objFile = fs.open(sFileName)
                    csvFileTRN = objFile.read().decode('utf8')

                else:
                    print('run_alg::after')
                    
                    dictStatus['bProc2nd'] = 'true'

                    # load train set
                    sFileName = session['sFileNameTrnA']
                    print("load_train", sFilePath, sFileName)
                    objFile = fs.open(sFileName)
                    csvFileTRN = objFile.read().decode('utf8')

                # load test set
                sCurLocation = settings.MEDIA_ROOT + TestsetPath
                fs = FileSystemStorage(sCurLocation)
                sFilePath = sCurLocation
                sFileName = TestsetFile
                print("load_test", sFilePath, sFileName)
                objFile = fs.open(sFileName)
                csvFileTST = objFile.read().decode('utf8')

                # make dataframe
                dataFrmTRN = pd.read_csv(io.StringIO(csvFileTRN), names = arrColumns, sep=r'\s*,\s*', skiprows=[0], engine='python', na_values = "?")
                dataFrmTST = pd.read_csv(io.StringIO(csvFileTST), names = arrColumns, sep=r'\s*,\s*', skiprows=[0], engine='python', na_values = "?")

                for col in dataFrmTRN.columns:
                    dataFrmTRN[col] = dataFrmTRN[col].replace(np.nan, 0)

                print('train rows: ', dataFrmTRN.shape[0], ' cells: ', dataFrmTRN.shape[1])
                print('test  rows: ', dataFrmTST.shape[0], ' cells: ', dataFrmTST.shape[1])

                # return train file name if pre-proc
                if dictStatus['bProc1st'] == 'true':
                    dictResult['filename'] = session['sFileNameTrnB']
                    
                st = time.time()

                arrConfMatrix = []
                arrHMapElemnt = []
                arrHtmlImgTag = []
                if AlgorithmType == 'gi':
                    arrConfMatrix, arrHMapElemnt, arrHtmlImgTag = alg_tpr.run_gi(json_data, dataFrmTRN, dataFrmTST)
                else:
                    arrConfMatrix, arrHMapElemnt, arrHtmlImgTag = alg_tpr.run_sklearn(json_data, dataFrmTRN, dataFrmTST)
                    
                print("alg_tpr::ProcTime {} sec".format(time.time()-st))

                arrTemp0 = session['arrConfMatrix']
                arrTemp1 = session['arrHMapElemnt']
                arrTemp2 = session['arrHtmlImgTag']
                arrTemp0.append(arrConfMatrix)
                arrTemp1.append(arrHMapElemnt)
                arrTemp2.append(arrHtmlImgTag)
                session['arrConfMatrix'] = arrTemp0
                session['arrHMapElemnt'] = arrTemp1
                session['arrHtmlImgTag'] = arrTemp2
                
                dictResult['data'] = {}
                dictResult['data']['confmat'] = session['arrConfMatrix']
                if session['bApiMode'] == 'false':
                    dictResult['data']['htmlimg'] = session['arrHtmlImgTag']
                dictResult['success'] = 'true'

                if dictStatus['bProc2nd'] == 'true':
                    dictStatus['bRunning'] = 'false'
                session['dictStatus'] = dictStatus
                session.save()

    else:
        dictResult['message'] = 'not POST method'
    
    print('run_alg::res', dictResult['success'])
    if 'message' in dictResult:
        print(dictResult['message'])
    return JsonResponse(json.dumps(dictResult), safe = False)

# external functions
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
    
    print('isrunning::res', dictResult)
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def run(request):
    print('run::req')
    
    global g_dictAlgParam

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
                session['bApiMode'] = 'true'
                
                if 'filename' not in json_data:
                    dictResult['message'] = 'filename is none'
                    return JsonResponse(json.dumps(dictResult), safe = False)
                sFileNameTrn = json_data['filename']
                print(sFileNameTrn)
                
                if 'dictAlgParam' not in session:
                    session['dictAlgParam'] = g_dictAlgParam
                dictAlgParam = session['dictAlgParam']
                # parameters
                bTrafficData = (sFileNameTrn.find('traffic') >= 0)
                dictAlgParam['key'] = key
                if bTrafficData == True:
                    dictAlgParam['ClassifyCol']  = 'damage'
                    dictAlgParam['ClassifyVals'] = ['0','1']
                    dictAlgParam['ClassifyLbls'] = ['Over $1,500', 'Less than $1,500']
                    dictAlgParam['SubGroupCol']  = 'posted_speed_limit'
                    dictAlgParam['SubGroupVals'] = ['0','1']
                    dictAlgParam['AlgorithmType']= 'lr'
                    dictAlgParam['AlgParameters']= {} # for 'gi'
                    dictAlgParam['RunTsneUmap']  = 'off'
                    dictAlgParam['TestsetPath']  = '/csv/traffic/'
                    dictAlgParam['TestsetFile']  = 'traffic_testset.csv'
                else:
                    dictAlgParam['ClassifyCol']  = 'sex'
                    dictAlgParam['ClassifyVals'] = ['0','1']
                    dictAlgParam['ClassifyLbls'] = ['Male', 'Female']
                    dictAlgParam['SubGroupCol']  = 'cva'
                    dictAlgParam['SubGroupVals'] = ['0','1']
                    dictAlgParam['AlgorithmType']= 'lr'
                    dictAlgParam['AlgParameters']= {} # for 'gi'
                    dictAlgParam['RunTsneUmap']  = 'off'
                    dictAlgParam['TestsetPath']  = '/csv/health/'
                    dictAlgParam['TestsetFile']  = 'health_testset.csv'
                session['dictAlgParam'] = dictAlgParam
                session.save()

                base_url =  "{0}://{1}/Fairness/".format(request.scheme, request.get_host())
                
                url = base_url + 'loading/'
                dictParam = {'key': key, 'filename': sFileNameTrn, 'bOriginData': True}
                res = requests.post(url, data = json.dumps(dictParam))

                if res.status_code == 200:
                    json_res = json.loads(res.json())
                    if json_res['success'] == 'true':
                        url = base_url + 'run_alg/'
                        res = requests.post(url, data = json.dumps(dictAlgParam))

                        if res.status_code == 200:
                            json_res = json.loads(res.json())
                            print(json_res)
                            if json_res['success'] == 'true':
                                
                                confmat = json_res['data']['confmat'][0]
                                
                                # url = "http://164.125.37.214:5555/api/fairness/tpr"
                                # if bTrafficData == False: url = url + "2"
                                # dictParam = {'csv': json_res['filename'], 'tpra': confmat[0]['TPR'], 'tprb': confmat[1]['TPR'] }
                                # print(dictParam)
                                # headers = {"Content-Type":"application/json"}
                                # try:
                                #     res = requests.get(url, params=dictParam, headers = headers)
                                #     res.raise_for_status()
                                # except requests.exceptions.HTTPError as err:
                                #     dictResult['message'] = err.response.text
                                #     print('run::res', dictResult)
                                #     return JsonResponse(json.dumps(dictResult), safe = False)

                                # if res.status_code == 200:
                                #     json_res = res.json()
                                #     print(json_res)
                                #     if 'train' in json_res and json_res['train'] != '':

                                #         url = base_url + 'loading/'
                                #         dictParam = {'key': key, 'filename': json_res['train'], 'bOriginData': False }
                                #         res = requests.post(url, data = json.dumps(dictParam))

                                #         if res.status_code == 200:
                                #             json_res = json.loads(res.json())
                                #             if json_res['success'] == 'true':
                                #                 url = base_url + 'run_alg/'
                                #                 res = requests.post(url, data = json.dumps(dictAlgParam))

                                #                 json_res = json.loads(res.json())
                                #                 if json_res['success'] == 'true':

                                #                     dictResult['success'] = 'true'
                                
                                # for test
                                url = base_url + 'loading/'
                                if bTrafficData == True:
                                    filename = 'traffic_after.csv'
                                else:
                                    filename = 'health_after.csv'
                                dictParam = {'key': key, 'filename': filename, 'bOriginData': False }
                                res = requests.post(url, data = json.dumps(dictParam))

                                if res.status_code == 200:
                                    json_res = json.loads(res.json())
                                    if json_res['success'] == 'true':
                                        url = base_url + 'run_alg/'
                                        res = requests.post(url, data = json.dumps(dictAlgParam))

                                        json_res = json.loads(res.json())
                                        if json_res['success'] == 'true':

                                            dictResult['success'] = 'true'

    else:
        dictResult['message'] = 'not POST method'
        
    print('run::res', dictResult)
    return JsonResponse(json.dumps(dictResult), safe = False)


@csrf_exempt
def getresult(request):
    
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
                    print('getresult::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                dictStatus = session['dictStatus']
                if dictStatus['bProc2nd'] == 'false':
                    dictResult['message'] = 'process not running'
                    print('getresult::res', dictResult)
                    return JsonResponse(json.dumps(dictResult), safe = False)
                
                dictResult['success'] = 'true'

                dictResult['data'] = {}
                dictResult['data']['corrate'] = []
            
                arrConfMatrix = session['arrConfMatrix']
                BA = arrConfMatrix[0][0]
                BB = arrConfMatrix[0][1]
                AA = arrConfMatrix[1][0]
                AB = arrConfMatrix[1][1]
                print(f"\n BA: {BA} \n BB: {BB} \n AA: {AA} \n AB: {AB} \n")
                # 1. binary
                # 1.1 equality of opportunity
                # core = (abs(before_tpr_a-before_tpr_b)-abs(after_tpr_a-after_tpr_b))/abs(before_tpr_a-before_tpr_b)
                fValB = abs( float(BA['TPR']) - float(BB['TPR']) )
                fValA = abs( float(AA['TPR']) - float(AB['TPR']) )
                fValR = (fValB - fValA) / fValB * 100.0
                dictResult['data']['corrate'].append( {'key': 'binaryequaloppo', 'name': '이진 >> 균등 기회', 'rate': str(fValR)} )
                # 1.2 Equalized Odds
                # core = ((abs(before_tpr_a-before_tpr_b)-abs(after_tpr_a-after_tpr_b))/abs(before_tpr_a-before_tpr_b)
                # + (abs(before_fpr_a-before_fpr_b)-abs(after_fpr_a-after_fpr_b))/abs(before_fpr_a-before_fpr_b) ) / 2
                fValTB = abs( float(BA['TPR']) - float(BB['TPR']) )
                fValTA = abs( float(AA['TPR']) - float(AB['TPR']) )
                fValFB = abs( float(BA['FPR']) - float(BB['FPR']) )
                fValFA = abs( float(AA['FPR']) - float(AB['FPR']) )
                if fValTB == 0:
                    fValT = 0
                else:
                    fValT = (fValTB - fValTA) / fValTB
                if fValFB == 0:
                    fValF = 0
                else:
                    fValF = (fValFB - fValFA) / fValFB
                fValR = (fValT + fValF) / 2 * 100.0
                dictResult['data']['corrate'].append( {'key': 'binaryequalodds', 'name': '이진 >> 균등 승률', 'rate': str(fValR)} )
                # 1.3 Demographic Parity
                # core = ( abs((before_tp_a+before_fp_a)/(before_tn_a+before_fn_a)-(before_tp_b+before_fp_b)/(before_tn_b+before_fn_b))
                # - abs((after_tp_a+after_fp_a)/(after_tn_a+after_fn_a)-(after_tp_b+after_fp_b)/(after_tn_b+after_fn_b)) )
                # / abs((before_tp_a+before_fp_a)/(before_tn_a+before_fn_a)-(before_tp_b+before_fp_b)/(before_tn_b+before_fn_b))
                fValBA = 0
                fDenom = ( int(BA['TP']) + int(BA['FP']) )
                if fDenom != 0:
                    fValBA = ( int(BA['TN']) + int(BA['FN']) ) / fDenom

                fValBB = 0
                fDenom = ( int(BB['TP']) + int(BB['FP']) )
                if fDenom != 0:
                    fValBB = ( int(BB['TN']) + int(BB['FN']) ) / fDenom
                
                fValAA = 0
                fDenom = ( int(AA['TP']) + int(AA['FP']) )
                if fDenom != 0:
                    fValAA = ( int(AA['TN']) + int(AA['FN']) ) / fDenom

                fValAB = 0
                fDenom = ( int(AB['TP']) + int(AB['FP']) )
                if fDenom != 0:
                    fValAB = ( int(AB['TN']) + int(AB['FN']) ) / fDenom

                print(f"BA: {fValBA} BB: {fValBB} AA: {fValAA} AB: {fValAB}")

                fValB = abs( fValBA - fValBB )
                fValA = abs( fValAA - fValAB )
                
                print(f"B: {fValB} A: {fValA}")
                
                fValR = 0
                if fValB != 0:
                    fValR = (fValB - fValA) / fValB * 100.0
                dictResult['data']['corrate'].append( {'key': 'binarydmgparity', 'name': '이진 >> 인구 통계 패리티', 'rate': str(fValR)} )
                # 2. retrun
                # 2.1 Demographic Parity
                fValR = '0.0'
                dictResult['data']['corrate'].append( {'key': 'regresdmgparity', 'name': '회귀 >> 인구 통계 패리티', 'rate': str(fValR)} )
                # 2.2 Bounded group loss
                fValR = '0.0'
                dictResult['data']['corrate'].append( {'key': 'regresbndgrplos', 'name': '회귀 >> 제한된 그룹 손실', 'rate': str(fValR)} )
                
                print(dictResult['data']['corrate'])

                dictResult['data']['heatmap'] = session['arrHMapElemnt']
                dictResult['data']['htmlimg'] = session['arrHtmlImgTag']

    else:
        dictResult['message'] = 'not POST method'
        
    return JsonResponse(json.dumps(dictResult), safe = False)


from django.http import HttpResponse, HttpResponseNotFound
@csrf_exempt
def getfile(request):

    if request.method == 'POST':
        json_data = json.loads(request.body)
        key = ''
        if 'key' not in json_data:
            response = HttpResponseNotFound('<h1>key is not set</h1>')
        else:
            key = json_data['key']
            session = get_session(key)
            if session == None:
                response = HttpResponseNotFound('<h1>invalid key</h1>')
            else:
                if 'dictStatus' not in session:
                    response = HttpResponseNotFound('<h1>dictStatus not exist</h1>')
                dictStatus = session['dictStatus']
                if dictStatus['bProc2nd'] == 'false':
                    response = HttpResponseNotFound('<h1>process not running</h1>')

                file_name = session['sFileNameTrnA']
                file_location = settings.MEDIA_ROOT + "result/" + file_name
                print(file_location)
                try:
                    with open(file_location, 'r') as f:
                        file_data = f.read()

                        response = HttpResponse(file_data, content_type='text/csv')
                        response['Content-Disposition'] = f'attachment; filename="{file_name}"'

                except IOError:
                    response = HttpResponseNotFound('<h1>File not exist</h1>')

    print(response)
    return response