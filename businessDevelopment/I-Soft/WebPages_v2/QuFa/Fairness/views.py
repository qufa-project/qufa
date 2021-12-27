
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
g_sCurLocation = settings.MEDIA_ROOT
g_dictAlgParam = {
    'key': '', 'AlgorithmType': 'gi',
    'SmplsPerVal': '1000',
    'ClassifyCol': 'damage',
    'ClassifyVals': ['<=1500', '>1500'],
    'ClassifyLbl': ['Less than $1,500', 'Over $1,500'],
    'SubGroupCol': 'crash_type',
    'SubGroupVal': ['INJURY AND / OR TOW DUE TO CRASH', 'NO INJURY / DRIVE AWAY'],
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

    global g_dictAlgParam
    
    dictResult = {}
    dictResult['success'] = 'false'
    dictResult['key'] = None

    if request.method == 'POST':
        
        session = SessionStore()
        session.create()
        session.set_expiry(0)
        
        dictAlgParam = g_dictAlgParam
        dictAlgParam['key'] = session.session_key
        session['dictAlgParam'] = dictAlgParam
        
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

                if 'arrDataCsv' not in session:
                    dictResult['message'] = 'arrDataCsv not exist'
                    print('sunburst::res', dictResult)    
                    return JsonResponse(json.dumps(dictResult), safe = False)
                    
                arrDataCsv = session['arrDataCsv']

                arrProcCsv = []
                if dictStatus['bApiMode'] == 'true':
                    arrProcCsv = arrDataCsv
                else:
                    if dictStatus['bLoad2nd'] == 'true':
                        arrProcCsv.append(arrDataCsv[1])
                    else:
                        arrProcCsv.append(arrDataCsv[0])
                
                st = time.time()

                arrResult = []            
                for dataCsv in arrProcCsv:
                    
                    dataFrm = pd.DataFrame(dataCsv['rows'], columns = dataCsv['columns'])

                    dictData = { 'name': 'root', 'children': [] }
                    for sColM in dataFrm.columns:
                        dictItem0 = { 'name': sColM, 'children': [] }
                        for sUnqM in np.sort(dataFrm[sColM].unique()):
                            dictItem1 = { 'name': sUnqM, 'children': [] }
                            
                            arrSubCol = list(dataFrm.columns)
                            arrSubCol.remove(sColM)
                            dfSub = dataFrm.loc[dataFrm[sColM]==sUnqM][arrSubCol]
                            for sColS in arrSubCol:
                                dictItem2 = { 'name': sColS, 'children': [] }
                                for sUnqS in np.sort(dfSub[sColS].unique()):
                                    dfUnq = dfSub.loc[dfSub[sColS]==sUnqS][sColS]
                                    sVal = str(len(dfUnq.tolist()))                                
                                    dictItem2['children'].append({'name': f"{sUnqS} ({sVal})", 'value': sVal})
                                    
                                dictItem1['children'].append(dictItem2)

                            dictItem0['children'].append(dictItem1)

                        dictData['children'].append(dictItem0)

                    arrResult.append(dictData)
                
                # arrThread = []
                # arrResult = []
                # for dataCsv in arrProcCsv:                    
                #     dataFrm = pd.DataFrame(dataCsv['rows'], columns = dataCsv['columns'])

                #     dictData = { 'name': 'root', 'children': [] }
                #     for sCol in dataFrm.columns:
                #         thread = clsThread(target=threadProc0, args=(dataFrm, sCol))
                #         dictThread = { 'key': sCol, 'thread': thread }
                #         arrThread.append(dictThread)
                #         thread.start()

                #     for item in arrThread:
                #         dictItem = { 'name': item['key'], 'children': [] }
                #         dictItem['children'] = item['thread'].join()
                #         dictData['children'].append(dictItem)
                        
                #     arrResult.append(dictData)
                    
                print("sunburst::ProcTime {} sec".format(time.time()-st))

                dictResult['success'] = 'true'
                dictResult['data'] = arrResult
                                
    else:
        dictResult['message'] = 'not POST method'

    print('sunburst::res', dictResult['success'])    
    return JsonResponse(json.dumps(dictResult), safe = False)

def threadProc0(dataFrm, sCol):
    arrReturn = []
    
    for sUnqM in np.sort(dataFrm[sCol].unique()):
        dictItemM = { 'name': sUnqM, 'children': [] }
        
        arrSubCol = list(dataFrm.columns)
        arrSubCol.remove(sCol)
        dfSub = dataFrm.loc[dataFrm[sCol]==sUnqM][arrSubCol]

        # for sColS in arrSubCol:
        #     dictItemS = { 'name': sColS, 'children': [] }            
        #     for sUnqS in np.sort(dfSub[sColS].unique()):
        #         dfUnq = dfSub.loc[dfSub[sColS]==sUnqS][sColS]
        #         sVal = str(len(dfUnq.tolist()))
        #         dictItemS['children'].append({'name': f"{sUnqS} ({sVal})", 'value': sVal})                
        #     dictItemM['children'].append(dictItemS)
            
        arrThread = []
        for sColS in arrSubCol:
            thread = clsThread(target=threadProc1, args=(dfSub, sColS))
            dictThread = { 'key': sColS, 'thread': thread }
            arrThread.append(dictThread)
            thread.start()

        for item in arrThread:
            dictItemS = { 'name': item['key'], 'children': [] }
            dictItemS['children'] = item['thread'].join()
            dictItemM['children'].append(dictItemS)

        arrReturn.append(dictItemM)

    return arrReturn

def threadProc1(dataFrm, sCol):
    arrReturn = []
            
    for sUnq in np.sort(dataFrm[sCol].unique()):
        dfUnq = dataFrm.loc[dataFrm[sCol]==sUnq][sCol]
        sVal = str(len(dfUnq.tolist()))
        arrReturn.append({'name': f"{sUnq} ({sVal})", 'value': sVal})
            
    return arrReturn

class clsThread(Thread):
    def __init__(self, group=None, target=None, name=None, args=(), kwargs={}, Verbose=None):
        Thread.__init__(self, group, target, name, args, kwargs)
        self._return = None
    def run(self):
        print(type(self._target))
        if self._target is not None:
            self._return = self._target(*self._args, **self._kwargs)
    def join(self, *args):
        Thread.join(self, *args)
        return self._return


@csrf_exempt
def run_alg(request):
    print('run_alg::req')
    
    global g_sCurLocation
    global g_dictAlgParam
    
    dictResult = {}
    dictResult['success'] = 'false'

    if request.method == 'POST':            
        dictAlgParam = json.loads(request.body)
        if 'key' not in dictAlgParam:
            dictResult['message'] = 'key is not set'            
        else:
            session = get_session(dictAlgParam['key'])
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
                    return JsonResponse(json.dumps(dictResult), safe = False)
                        
                objRcvFile = session['objRcvFile']
                arrColumns = session['arrColumns']
                arrDataCsv = copy.deepcopy(session['arrDataCsv'])
                # parameters
                ClassifyCol = dictAlgParam['ClassifyCol'] # 'damage'
                ClassifyVals = dictAlgParam['ClassifyVals'] # ['>1500','<=1500']

                fs = FileSystemStorage(g_sCurLocation)
                sFilePath = g_sCurLocation
                
                if dictStatus['bProc1st'] == 'false':
                    print('run_alg::before')
                    
                    dictStatus['bProc1st'] = 'true'

                    # make test set
                    nLimit = int(dictAlgParam['SmplsPerVal']) #5000
                    nCount0 = 0
                    nCount1 = 0
                    arrSample0 = []
                    arrSample1 = []

                    for idx, row in enumerate(arrDataCsv[0]['rows']):

                        if row[ClassifyCol] == ClassifyVals[0]:
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
                    print('run_alg::after')
                    
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

                for col in dataFrmTRN.columns:
                    dataFrmTRN[col] = dataFrmTRN[col].replace(np.nan, 0)

                print('train rows: ', dataFrmTRN.shape[0], ' cells: ', dataFrmTRN.shape[1])
                print('test  rows: ', dataFrmTST.shape[0], ' cells: ', dataFrmTST.shape[1])

                # return train file name if pre-proc
                if dictStatus['bProc1st'] == 'true':
                    dictResult['filename'] = session['sFileNameTrnB']
                    
                st = time.time()

                sAlgType = 'gi'
                if 'AlgorithmType' in dictAlgParam:
                    sAlgType = dictAlgParam['AlgorithmType']
                if sAlgType == 'gi':
                    arrDataTpr, arrDataCMx, arrMetric, arrImgTag = alg_tpr.run_gi(dictAlgParam, dataFrmTRN, dataFrmTST)
                else:
                    arrDataTpr, arrDataCMx, arrMetric, arrImgTag = alg_tpr.run_sklearn(dictAlgParam, dataFrmTRN, dataFrmTST)
                    
                print("alg_tpr::ProcTime {} sec".format(time.time()-st))

                arrTmpDataTpr = session['arrDataTpr']
                arrTmpDataCMx = session['arrDataCMx']
                session['arrDataTpr'] = arrTmpDataTpr + arrDataTpr
                session['arrDataCMx'] = arrTmpDataCMx + arrDataCMx
                
                dictResult['Metric'] = arrMetric
                dictResult['ImgTag'] = arrImgTag
            
                if dictStatus['bProc2nd'] == 'true':
                    dictStatus = {'bApiMode': 'false', 'bRunning': 'false', 'bLoad1st': 'false', 'bLoad2nd': 'false', 'bProc1st': 'false', 'bProc2nd': 'false'}
                
                session['dictStatus'] = dictStatus
                session.save()

                dictResult['success'] = 'true'

    else:
        dictResult['message'] = 'not POST method'
        
    print('run_alg::res', dictResult['success'])
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
                
                dictAlgParam = g_dictAlgParam
                if 'dictAlgParam' not in session:
                    dictAlgParam['key'] = key
                    session['dictAlgParam'] = dictAlgParam
                    session.save()
                dictAlgParam = session['dictAlgParam']

                #base_url =  "{0}://{1}{2}".format(request.scheme, request.get_host(), request.path)
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
                            if json_res['success'] == 'true':
                                
                                aTPR = []
                                for sIdx in json_res['Metric']:
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
                                                url = base_url + 'run_alg/'
                                                res = requests.post(url, data = json.dumps(dictAlgParam))

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
                                #         url = base_url + 'run_alg/'
                                #         res = requests.post(url, data = json.dumps(dictAlgParam))

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
