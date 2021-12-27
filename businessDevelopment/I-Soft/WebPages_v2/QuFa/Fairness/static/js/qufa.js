// global variable
let g_arrBtnText = [];
let g_strTagWork = "<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>";
let g_strTagBusy = "<i class='fas fa-spinner fa-pulse' style='font-size:100px;color:#CCC;'></i>";
let g_regExp = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\ '\"\\(\=]/gi;
let g_wkrAlgProc = null;
let g_sAccessKey = '';
let g_bManualRun = false;
let g_bOriginData = true;
let g_bProcDone = true;
let g_clsDatasetFair = null;
let g_arrDataSunburst = null;
const g_cD3scaleOrd = d3.scaleOrdinal(d3.schemePaired);//d3.schemeCategory10);//
const g_arrAlgType = 
[
    { key: 'gi' , name: 'Tensorflow Keras' },
    { key: 'lr' , name: 'Logistic Regression' },
    { key: 'sdg', name: 'SGD Classifier' },
    { key: 'svm', name: 'Linear SVC' }
];
// for test
let bChicagoData = true;
let arrColHdr0 = ['posted_speed_limit', 'weather_condition', 'lighting_condition',
                'first_crash_type', 'roadway_surface_cond', 'crash_type', 'damage'];
let arrClsLbl0 = ['Less than $1,500','Over $1,500'] // ['<=1500','>1500']
let arrColHdr1 = ['sex', 'age', 'cva', 'fcvayn', 'packyear', 'sd_idr2', 'exerfq'];
let arrClsLbl1 = ['Male','Female'] // ['0','1']

$(document).on('click', 'a[href*=\\#]', function(event)
{
    event.preventDefault();
    $('html, body').animate({ scrollTop: $(this.hash).offset().top }, 500);
});
$(document).ready(function()
{
    function csrfSafeMethod(method) { return (/^(GET|HEAD|OPTIONS|TRACE)$/.test(method)); }
    $.ajaxSetup({beforeSend:function(xhr, settings){if(!csrfSafeMethod(settings.type) && !this.crossDomain){xhr.setRequestHeader("X-CSRFToken", csrfTtoken);}}});

    Chart.defaults.global.legend.display = false;
    
    $(".sticky-header").floatThead({position:'fixed', top:60, zIndex:100});
    
    g_arrBtnText.push( $('#idBtnLoading').html() );
    g_arrBtnText.push( $('#idBtnRun').html() );
    
    $('.running').prop("disabled",true).addClass('disabled');

    $("#idDivParamTpr").hide();
    $("#idDivParamGi").hide();
    
    $.ajax(
    {
        url: "getkey/",
        type: 'POST',
        dataType: 'JSON',
        success: function(res)
        {
            var res = JSON.parse(res);
            if ( res.success == 'true' )
            {
                g_sAccessKey = res.key;
            }
        }
    })
    
    let sTag = '';
    let sVal = '';
    let sCID = '';

    var objFile = null;
    var handleFileSelect = function(e)
    {
        if ( !e.target.files ) return;
        
        objFile = e.target.files[0];

        $("#idFileName").val(objFile.name);
    }
    $("#fileupload")[0].addEventListener('change', handleFileSelect, false);

    $('#idChkManualRun').prop('checked', g_bManualRun);
    $('#idChkManualRun').on('click', function(e)
    {
        g_bManualRun = $('#idChkManualRun').prop('checked');        
        var html = ( g_bManualRun ) ? ' (BEFORE)' : '';
        $('#idBtnRun').html(g_arrBtnText[1] + html);
    });
    $('#idBtnUpload').on('click', function(e)
    {
        e.preventDefault();

        if ( objFile != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('#idBtnLoading').html(g_strTagWork).addClass('fileupload-processing');
    
            $('#idFileName').val('');
            $('#fileupload').val('');

            let formData = new FormData();
            formData.append('csrfmiddlewaretoken', csrfTtoken);
            formData.append('file', objFile);
            formData.append('key', g_sAccessKey);
            $.ajax(
            {
                url: "upload/",
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function(res)
                {
                    var res = JSON.parse(res);
                    if ( res.success == 'true' )
                    {
                        $('#idAStep1').trigger('click');  

                        fnCheckFileList(true);

                        $('.loading').prop("disabled",false).removeClass('disabled');
                        $('#idBtnLoading').html(g_arrBtnText[0]).removeClass('fileupload-processing');
                    }
                }
            })
        }
        else
        {
            alert("Train 파일이 지정되지 않았습니다.");
            return;
        }
    });
    $('#idBtnLoading').on('click', function(e)
    {
        e.preventDefault();

        let sFileName = $( "#idSelFileList option:selected" ).val();
        if ( sFileName != undefined )
        {
            // for test
            bChicagoData = sFileName.includes('chicago');

            fnLoadFile(sFileName);
        }
        else
        {
            alert("Train 파일이 지정되지 않았습니다.");
            return;
        }
    });
    $('#idBtnRun').on('click', function(e)
    {
        e.preventDefault();

        if ( acceessableCount <= 0 ) return;
        acceessableCount = acceessableCount - 1;

        if ( g_clsDatasetFair != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            $('#idBtnRun').html(g_strTagWork).addClass('fileupload-processing');
            
            let objData = {};
            objData['key'] = g_sAccessKey;
            objData['csrfTtoken'] = csrfTtoken;
                        
            objData['AlgorithmType'] = $('#idSelTprAlrotithm option:selected').val();
            objData['SmplsPerVal'] = $('#idTxtSamplesPerVal').val();
            objData['ClassifyCol'] = $('#idSelCol4Classify option:selected').text();
            let arrVal = [];
            $('#idUlClassifyVals li').each(function()
            {
                var sVal = $(this).text();
                arrVal.push(sVal);
            });
            objData['ClassifyVals'] = arrVal;//.sort(function(a, b){return a - b});
            objData['ClassifyLbls'] = [ $( "#idTxtClassifyLblA" ).val(), $( "#idTxtClassifyLblB" ).val() ];
            objData['SubGroupCol'] = $('#idSelCol4SubGroup option:selected').text();
            arrVal = [];
            $('#idUlSubGroupVals li').each(function()
            {
                var sVal = $(this).text();
                arrVal.push(sVal);
            });
            objData['SubGroupVals'] = arrVal;//.sort(function(a, b){return a - b});

            if ( objData['AlgorithmType'] == 'gi')
            {
                objData['Parameters'] = [];
                objData['Parameters'].push( $( "#idTxtParam00" ).val() );
                objData['Parameters'].push( $( "#idTxtParam01" ).val() );
                objData['Parameters'].push( $( "#idTxtParam02" ).val() );
                objData['Parameters'].push( $( "#idTxtParam03" ).val() );
                objData['Parameters'].push( $( "#idTxtParam04" ).val() );
                objData['Parameters'].push( $( "#idTxtParam05" ).val() );
                objData['Parameters'].push( $( "#idTxtParam06" ).val() );

                objData['HashBucketSize'] = $('#idTxtHashBkSize').val();
                objData['Categorfeatures'] = [];
                objData['Numericfeatures'] = [];

                let aSelKey = [objData['ClassifyCol'], objData['SubGroupCol']];
                for (let sKey in g_clsDatasetFair.arrOvColumns)
                {
                    if ( !aSelKey.includes(sKey) )
                    {
                        let objItem = {};
                        objItem['ColName'] = sKey;
                        if ( !g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric )
                        {
                            let arrVal = [];
                            $('#idUlList' + sKey.replace(g_regExp,'') + ' li').each(function()
                            {
                                let sVal = $(this).attr('data-id');
                                if ( $('#idChkVal_'+sVal).prop('checked') )
                                {
                                    let val = $('#idTxtVal_'+sVal).val();
                                    arrVal.push(val);
                                };
                            });
                            if ( !arrVal.length )
                            {
                                alert("Categorical Feature의 Vocabulary List가 비었습니다.");            
                                $('.loading').prop("disabled",false).removeClass('disabled');
                                $('.running').prop("disabled",false).removeClass('disabled');
                                $('#idBtnRun').html(g_arrBtnText[1]).removeClass('fileupload-processing');
                                return;
                            }
                            objItem['VocList'] = arrVal.sort();
                            objData['Categorfeatures'].push(objItem);
                        }
                        else
                        {
                            let list = $('#idTxtList'+sKey.replace(g_regExp,'')).val();
                            let arrVal = (new Function("return [" + list + "];")());
                            if ( !arrVal.length )
                            {
                                alert("Numeric Feature의 Boundaries List가 비었습니다.");            
                                $('.loading').prop("disabled",false).removeClass('disabled');
                                $('.running').prop("disabled",false).removeClass('disabled');
                                $('#idBtnRun').html(g_arrBtnText[1]).removeClass('fileupload-processing');
                                return;
                            }                      
                            objItem['BndList'] = arrVal.sort(function(a, b){return a - b});
                            objData['Numericfeatures'].push(objItem);
                        }
                    }
                }
            }
            g_wkrAlgProc.postMessage(objData);
        }
        else
        {
            alert("로딩된 데이터가 없습니다.");

            $('.loading').prop("disabled",false).removeClass('disabled');
            $('.running').prop("disabled",false).removeClass('disabled');
            $('#idBtnRun').html(g_arrBtnText[1]).removeClass('fileupload-processing');
            
            return
        }
        acceessableCount = acceessableCount +1; 
    });
    
    $('#idAStep0').trigger('click');
    
    fnCheckFileList();

    g_wkrAlgProc = new Worker(sFilePath);
    g_wkrAlgProc.onmessage = function(e)
    {
        $('#idAStep5').trigger('click');

        let json_res = JSON.parse(JSON.parse(e.data));
        if ( !json_res.success )
        {
            alert( json_res.message );
            return;
        }
        
        if ( g_bOriginData )
        {
            let aTPR = [];
            for (let nIdx in json_res.Metric)
            {
                let sMetric = '';
                for (let sKey in json_res.Metric[nIdx])
                {
                    aTPR.push(json_res.Metric[nIdx][sKey])
                    sMetric += '<p>' + sKey + ': ' + json_res.Metric[nIdx][sKey] + '</p>';
                }
                $('#idDivMetricA'+nIdx).html(sMetric);
            }
            $('#idDivGraphA0').html(json_res.ImgTag[0]);
            $('#idDivGraphA1').html(json_res.ImgTag[1]);
            $('#idDivGraphA2').html(json_res.ImgTag[2]);
    
            var url = "http://164.125.37.214:5555/api/fairness/tpr"
            if ( !bChicagoData ) url += "2";
            var data =
            {
                csv: json_res.filename,//"210719_fairness_test_origin chicago crashes.csv",//
                tpra: aTPR[0],//"0.50",//
                tprb: aTPR[2],//"0.163",//
            };
            $.ajax(
            {
                url: url,
                dataType: 'JSON',
                method: 'GET',
                data: data,
                success: function(res)
                {
                    if ( res.train != '' )
                    {
                        fnLoadFile(res.train, false);
                    }
                    else
                    {
                        alert("로딩된 데이터가 없습니다.");
            
                        $('.loading').prop("disabled",false).removeClass('disabled');
                        $('.running').prop("disabled",false).removeClass('disabled');
                        $('#idBtnRun').html(g_arrBtnText[1]).removeClass('fileupload-processing');
                        
                        return
                    }
                },
                error: function(jqXHR, textStatus, errorThrown)
                {
                    //alert( jqXHR.status );
                    alert( jqXHR.statusText );
                    //alert( jqXHR.responseText );
                    //alert( jqXHR.readyState );
                }
            })

            // for test
            // fnLoadFile((bChicagoData)?"c_train_after.csv":"m_train_after.csv", false);
        }
        else
        {
            let aTPR = [];
            for (let nIdx in json_res.Metric)
            {
                let sMetric = '';
                for (let sKey in json_res.Metric[nIdx])
                {
                    aTPR.push(json_res.Metric[nIdx][sKey])
                    sMetric += '<p>' + sKey + ': ' + json_res.Metric[nIdx][sKey] + '</p>';
                }
                $('#idDivMetricB'+nIdx).html(sMetric);
            }
            $('#idDivGraphB0').html(json_res.ImgTag[0]);
            $('#idDivGraphB1').html(json_res.ImgTag[1]);
            $('#idDivGraphB2').html(json_res.ImgTag[2]);

            $.ajax(
            {
                url: 'getresult/',
                type: 'POST',
                data: JSON.stringify({'key': g_sAccessKey}),
                dataType: 'JSON',
                success: function(res)
                {
                    var res = JSON.parse(res);
                    var nPercentage = res.result.rate * 100.0;
                    sTag = '<p style="font-size:48px;">보정 성능 : ' + nPercentage.toFixed(3) + ' % ' + '</p>';
                    $('#idDivPerform').html(sTag);
                }
            })

            g_bProcDone = true;          

            fnCheckFileList(true);

            $('.loading').prop("disabled",false).removeClass('disabled');
            var html = ( g_bManualRun ) ? '(BEFORE)' : '';
            $('#idBtnRun').html(g_arrBtnText[1] + html).removeClass('fileupload-processing');
        }
    };
    g_wkrAlgProc.onerror = function(e)
    {
        alert("Error : " + e.message + " (" + e.filename + ":" + e.lineno + ")");
    };
});
function fnCheckFileList(update = false)
{
    $.ajax(
    {
        url: "getcount/",
        type: 'POST',
        data: JSON.stringify({'key': g_sAccessKey}),
        dataType: 'JSON',
        success: function(res)
        {
            var res = JSON.parse(res);
            if ( res.success == 'true' )
            {
                if ( res.count != $("#idSelFileList option").length || update )
                {
                    $.ajax(
                    {
                        url: "getlist/",
                        type: 'POST',
                        data: JSON.stringify({'key': g_sAccessKey}),
                        dataType: 'JSON',
                        success: function(res)
                        {
                            var res = JSON.parse(res);
                            var sTag = ''
                            $('#idSelFileList').empty();
                            if ( res.success == 'true' )
                            {
                                for (var nIdx in res.list)
                                {
                                    sTag = "<option value='" + res.list[nIdx].filename + "' style='height:40px;'>"
                                    + res.list[nIdx].filename + " ( " + res.list[nIdx].time + " )</option>";
                                    $('#idSelFileList').append(sTag);
                                }
                            }
                        }
                    })
                }
            }
        }
    })
}
function fnLoadFile(sFileTRN, bOriginData = true)
{
    g_bOriginData = bOriginData;

    if ( bOriginData )
    {
        g_bProcDone = false;
        
        $('.loading').prop("disabled",true).addClass('disabled');
        $('#idBtnLoading').html(g_strTagWork).addClass('fileupload-processing');
        
        $('#idDivSbChartA').html('');
        $('#idDivSbChartB').html('');

        $("#idDivParamTpr").hide();
        $("#idDivParamGi").hide();

        $('#idDivMetricA0').html('');
        $('#idDivMetricA1').html('');
        $('#idDivGraphA0').html('');
        $('#idDivGraphA1').html('');
        $('#idDivMetricB0').html('');
        $('#idDivMetricB1').html('');
        $('#idDivGraphB0').html('');
        $('#idDivGraphB1').html('');
        $('#idDivPerform').html('');
    }
    
    g_clsDatasetFair = null;
    g_arrDataSunburst = [];

    $.ajax(
    {
        url: "loading/",
        type: 'POST',
        data: JSON.stringify({'key': g_sAccessKey, 'filename': sFileTRN, 'bOriginData': bOriginData}),
        dataType: 'JSON',
        success: async function(res)
        {
            var res = JSON.parse(res);
            if ( res.success == 'true' )
            {
                $('#idAStep2').trigger('click');
                await fnPrepareOverView();

                $('#idAStep3').trigger('click');
                await fnPrepareSunburst();

                $('#idAStep4').trigger('click');
                await fnPrepareProc();
                
                if ( bOriginData )
                {
                    $('.loading').prop("disabled",false).removeClass('disabled');
                    $('#idBtnLoading').html(g_arrBtnText[0]).removeClass('fileupload-processing');
                    $('.running').prop("disabled",false).removeClass('disabled');
                }

                if ( !g_bProcDone )
                {
                    if ( g_bManualRun )
                    {
                        var html = (bOriginData) ? ' (BEFORE)':' (AFTER)';
                        $('#idBtnRun').html(g_arrBtnText[1] + html).removeClass('fileupload-processing');
                    }
                    else
                    {
                        $('#idBtnRun').trigger('click');
                    }
                }
            }
        }
    })
}

const arrBarColor = [ 'rgba(170, 200, 255, 0.9)', 'rgba(255, 170, 200, 0.9)' ];

function fnPrepareOverView()
{
    return new Promise(function(resolve, reject)
    {
        var sSpace = "<p style='height:20px;'></p>";
        $('#idDivNumeric').html(sSpace + g_strTagBusy);
        $('#idDivCategor').html(sSpace + g_strTagBusy);
        
        $.ajax(
        {
            url: "overview/",
            type: 'POST',
            data: JSON.stringify({'key': g_sAccessKey}),
            dataType: 'JSON',
            success: async function(res)
            {
                var res = JSON.parse(res);
                if ( res.success == 'true' )
                {
                    g_clsDatasetFair = res.data;
                    
                    $('#idTblNumeric tbody').empty();
                    $('#idTblCategor tbody').empty();

                    var nFileCnt = g_clsDatasetFair.objRawData.length;
                    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                    {
                        // Make Raw Data List     
                        $('#idTblRawData'+nIdxF.toString()+' thead').empty();      
                        var strContents = "<tr><th class='text-center fixed_th td-sm' style='width:3rem'>NO</th>";
                        for (var nCol in g_clsDatasetFair.objRawData[nIdxF].columns)
                        {
                            var sKey = g_clsDatasetFair.objRawData[nIdxF].columns[nCol];
                            strContents += "<th class='text-center fixed_th td-sm'>" + sKey + "</th>";
                        }
                        strContents += "</tr>";
                        $('#idTblRawData'+nIdxF.toString()+' thead').append(strContents);
                
                        $('#idTblRawData'+nIdxF.toString()+' tbody').empty();
                        strContents = '';
                        for (var nRow in g_clsDatasetFair.objRawData[nIdxF].rows)
                        {
                            var nNum = +nRow;
                            if ( isNaN(nNum) ) continue;
                            if ( nNum >= 5 ) break;
                            
                            strContents += "<tr><td class='text-right td-sm'>" + (nNum+1).toString() + "</td>";
                            for (var nCol in g_clsDatasetFair.objRawData[nIdxF].columns)
                            {
                                var sKey = g_clsDatasetFair.objRawData[nIdxF].columns[nCol];
                
                                strContents += "<td class='text-center td-sm'>";
                                strContents += g_clsDatasetFair.objRawData[nIdxF].rows[nRow][sKey];
                                strContents += '</td>';
                            }
                            strContents += '</tr>';
                        }
                        $('#idTblRawData'+nIdxF.toString()+' tbody').append(strContents);
                
                        // Make Overview
                        for (var nCol in g_clsDatasetFair.objRawData[nIdxF].columns)
                        {
                            var sKey = g_clsDatasetFair.objRawData[nIdxF].columns[nCol];
                
                            // Make Chart Data
                            g_clsDatasetFair.arrOvColumns[sKey].idCanvas = 'idCvsChart' + nCol.toString();
                            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl = [];
                            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal = [];
                            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr = [];
                            
                            for (var sVal in g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr)
                            {
                                var fVal = ( g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].bNumeric ) ? +sVal : sVal;
                                g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl.push( fVal );
                                g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal.push( +g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sVal] );
                                g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr.push( arrBarColor[nIdxF] );
                            }
                        }
                    }
                    // Make Table
                    var sTableItem = '';
                    for (var sKey in g_clsDatasetFair.arrOvColumns)
                    {
                        if ( g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric )
                        {
                            sTableItem = "<tr style='height:20px;'>"
                                + "<td colspan=7 class='font-weight-bold'>" + sKey + "</td>"
                                + "<td rowspan=" + (nFileCnt+1).toString() + "><div style='padding-left:15px;height:360px;'>"
                                + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].idCanvas + "' style='width:480px;height:320px;'></canvas>"
                                + "</div></td>"
                                + "</tr>"
                            $('#idTblNumeric').append(sTableItem);
                
                            for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                            {
                                var nRowCnt = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount;
                                var fPercentage = (g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nZeros/nRowCnt)*100;
                                sTableItem = "<tr>"
                                    + "<td class='text-center'>" + nRowCnt + "</td>"
                                    + "<td class='text-center'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "<td class='text-center'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fStdDev*100)/100).toFixed(2) + "</td>"
                                    + "<td class='text-center'>" + (Math.round(fPercentage *100)/100).toFixed(2) + "%</td>"
                                    + "<td class='text-center'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMin + "</td>"
                                    + "<td class='text-center'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMedian + "</td>"
                                    + "<td class='text-center'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMax + "</td>"
                                    + "</tr>";
                                $('#idTblNumeric').append(sTableItem);
                            }
                        }
                        else
                        {
                            sTableItem = "<tr style='height:20px;'>"
                                + "<td colspan=5 class='font-weight-bold'>" + sKey + "</td>"
                                + "<td rowspan=" + (nFileCnt+1).toString() + "><div style='padding-left:15px;height:360px;'>"
                                + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].idCanvas + "' style='width:480px;height:320px;'></canvas>"
                                + "</div></td>"
                                + "</tr>";
                            $('#idTblCategor').append(sTableItem);
                            
                            for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                            {
                                var sMaxKey = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].sMaxUniqueKey;
                                sTableItem = "<tr>"
                                    + "<td class='text-center'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount + "</td>"
                                    + "<td class='text-center'>" + Object.keys(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr).length + "</td>"
                                    + "<td class='text-center'>" + sMaxKey + "</td>"
                                    + "<td class='text-center'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sMaxKey] + "</td>"
                                    + "<td class='text-center'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "</tr>";
                                $('#idTblCategor').append(sTableItem);
                            }
                        }
                    }
                    fnCreateChart();

                    return resolve(true);
                }
            }
        })
    });
}
function fnPrepareSunburst()
{
    return new Promise(function(resolve, reject)
    {
        var sSpace = "<p style='height:50px;'></p>";
        if ( g_bOriginData )
        {
            $('#idDivSbChartA').html(sSpace + g_strTagBusy);
        }
        else
        {
            $('#idDivSbChartB').html(sSpace + g_strTagBusy);
        }
        $.ajax(
        {
            url: "sunburst/",
            type: 'POST',
            data: JSON.stringify({'key': g_sAccessKey}),
            dataType: 'JSON',
            success: async function(res)
            {
                var res = JSON.parse(res);
                if ( res.success == 'true' )
                {
                    arrData = res.data;
                    if ( arrData.length > 0 )
                    {
                        g_arrDataSunburst.push(arrData[0]);
                        
                        if ( g_bOriginData )
                        {
                            $('#idDivSbChartA').html('');
                            Sunburst().data(arrData[0])
                            .width(400)
                            .height(400)
                            .color((d, parent) => g_cD3scaleOrd(parent ? parent.data.name : null))    
                            .excludeRoot(true)
                            .radiusScaleExponent(1)
                            (document.getElementById('idDivSbChartA'));
                        }
                        else
                        {                        
                            $('#idDivSbChartB').html('');
                            Sunburst().data(arrData[0])
                            .width(400)
                            .height(400)
                            .color((d, parent) => g_cD3scaleOrd(parent ? parent.data.name : null))    
                            .excludeRoot(true)
                            .radiusScaleExponent(1)
                            (document.getElementById('idDivSbChartB'));
                        }
                    }
                    
                    return resolve(true);
                }
            }
        });
    });
}
let acceessableCount = 1;
function fnPrepareProc()
{
    return new Promise(function(resolve, reject)
    { 
        let sTag = '';
        let sVal = '';

        $('#idSelTprAlrotithm').empty();
        $('#idSelCol4Classify').empty();
        $('#idSelCol4SubGroup').empty();

        for (var nIdx in g_arrAlgType)
        {
            sTag = "<option value='" + g_arrAlgType[nIdx].key + "'>" + g_arrAlgType[nIdx].name + "</option>";
            $('#idSelTprAlrotithm').append(sTag);
        }

        for (var sKey in g_clsDatasetFair.arrOvColumns)
        {
            sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
            $('#idSelCol4Classify').append(sTag);
            $('#idSelCol4SubGroup').append(sTag);
        }
        $('#idSelTprAlrotithm').on('change', function()
        {
            if ( $(this).val() == 'gi')
            {
                $("#idDivParamGi").show();
            }
            else
            {
                $("#idDivParamGi").hide();
            }
        });
        $('#idSelCol4Classify').on('change', function()
        {
            let sSelKey = $( "#idSelCol4Classify option:selected" ).text();
            sVal = '';
            $('#idUlClassifyVals').empty();
            for (var nIdx in g_clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl)
            {
                sVal = g_clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl[nIdx];
                sTag = "<li>" + sVal + "</li>";
                $('#idUlClassifyVals').append(sTag);
            }
            $('#idSelCol4SubGroup').empty();
            for (var sKey in g_clsDatasetFair.arrOvColumns)
            {
                //if ( !g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && sKey != sSelKey )
                if ( sKey != sSelKey )
                {
                    sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
                    $('#idSelCol4SubGroup').append(sTag);
                }
            }
            sVal = $("#idSelCol4SubGroup option:first").val();
            $('#idSelCol4SubGroup').val(sVal).trigger('change');
        });
        $('#idSelCol4SubGroup').on('change', function()
        {
            var sSelKey = $( "#idSelCol4SubGroup option:selected" ).text();
            sVal = '';
            $('#idUlSubGroupVals').empty();
            for (var nIdx in g_clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl)
            {
                sVal = g_clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl[nIdx];
                sTag = "<li>" + sVal + "</li>";
                $('#idUlSubGroupVals').append(sTag);
            }
            fnFillCatgNumr();
        });
        if ( $("#idSelCol4SubGroup option").length > 0 )
        {
            sVal = $("#idSelCol4SubGroup option:first").val();
            $('#idSelCol4SubGroup').val(sVal).trigger('change');
            
            // for test
            if ( bChicagoData )
            {
                $('#idSelCol4SubGroup').val(arrColHdr0[5]).trigger('change');
                $('#idTxtClassifyLblA').val(arrClsLbl0[0]);
                $('#idTxtClassifyLblB').val(arrClsLbl0[1]);
            }
            else
            {
                $('#idSelCol4SubGroup').val(arrColHdr1[2]).trigger('change');
                $('#idTxtClassifyLblA').val(arrClsLbl1[0]);
                $('#idTxtClassifyLblB').val(arrClsLbl1[1]);
            }
        }

        sVal = $("#idSelCol4Classify option:first").val();
        $('#idSelCol4Classify').val(sVal).trigger('change');
        
        $("#idDivParamTpr").show(); 

        // for test
        $('#idSelTprAlrotithm').val(g_arrAlgType[3].key).trigger('change');
        if ( bChicagoData )
        {
            $('#idSelCol4Classify').val(arrColHdr0[6]).trigger('change');
            $('#idSelCol4SubGroup').val(arrColHdr0[5]).trigger('change');
        }
        else
        {
            $('#idSelCol4Classify').val(arrColHdr1[0]).trigger('change');
            $('#idSelCol4SubGroup').val(arrColHdr1[2]).trigger('change');
        }

        return resolve(true);
    });
}
function fnFillCatgNumr()
{
    let aSelKey = [$( "#idSelCol4Classify option:selected" ).text(), $( "#idSelCol4SubGroup option:selected" ).text()];
    
    sTag = '';
    for (let sKey in g_clsDatasetFair.arrOvColumns)
    {
        if ( !g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
        {
            sTag += "<h5 class='font-weight-bold text-left text-secondary'><div class='row'>" +
                "<div class='col-1'><i class='fas fa-angle-double-right'></i></div>" +
                "<div class='col-2' style='text-align:right;'>Vocabulary Column:</div>" +
                "<div class='col-3'>" +
                "<input type='text' id='idTxtCol" + sKey.replace(g_regExp,'') + "' class='running' style='width:100%;height:28px;' value='" + sKey + "' readonly />" +
                "</div>" +
                "<div class='col-2' style='text-align:right;'>Vocabulary List:</div>" +
                "<div class='col-3'>" +
                "<ul id='idUlList" + sKey.replace(g_regExp,'') + "' style='width:100%;'>" +
                "</ul></div><div class='col-1'></div></div></h5>";
        }
    }
    $('#idDivItem1').html(sTag);
    
    for (let sKey in g_clsDatasetFair.arrOvColumns)
    {
        if ( !g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
        {
            for (let nIdx in g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl)
            {
                sVal = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl[nIdx];
                sCID = sKey.replace(g_regExp,'') + "_" + sVal.replace(g_regExp,'');
                sTag = "<li data-id='"+ sCID +"'>" +
                    "<span style='display:inline-block;width:300px;'><input type='checkbox' id='idChkVal_"+sCID+"' class='running' checked />" +
                    "<label for='idChkVal_"+sCID+"'>&nbsp;"+sVal+"</label></span>" +
                    "<input type='hidden' id='idTxtVal_"+sCID+"' value='"+sVal+"' /></li>";
                $('#idUlList'+sKey.replace(g_regExp,'')).append(sTag);
            }
        }
    }
    
    sTag = '';
    for (let sKey in g_clsDatasetFair.arrOvColumns)
    {
        if ( g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
        {
            sTag += 
                "<h5 class='font-weight-bold text-left text-secondary'><div class='row'>" +
                "<div class='col-1'><i class='fas fa-angle-double-right'></i></div>" +
                "<div class='col-2' style='text-align:right;'>Numeric Column:</div>" +
                "<div class='col-3'>" +
                "<input type='text' id='idTxtCol" + sKey.replace(g_regExp,'') + "' class='running' style='width:100%;height:28px;' value='" + sKey + "' readonly />" +
                "</div>" +
                "<div class='col-2' style='text-align:right;'>Boundaries:</div>" +
                "<div class='col-3'>" +
                "<input type='text' id='idTxtList" + sKey.replace(g_regExp,'') + "' class='running' style='width:100%;height:28px;' value='10,20,30,40,50,60,70,80' />" +
                "</ul></div><div class='col-1'></div></div></h5>";
        }
    }
    $('#idDivItem2').html(sTag);
}
function fnCreateChart()
{
    for (var sKey in g_clsDatasetFair.arrOvColumns)
    {        
        if ( g_clsDatasetFair.arrOvColumns[sKey].ptrChart != null ) g_clsDatasetFair.arrOvColumns[sKey].ptrChart.destroy();

        var idCanvas = g_clsDatasetFair.arrOvColumns[sKey].idCanvas;
        g_clsDatasetFair.arrOvColumns[sKey].ctxCanvas = document.getElementById(idCanvas).getContext('2d');
        g_clsDatasetFair.arrOvColumns[sKey].ctxCanvas.canvas.width = 480;
        g_clsDatasetFair.arrOvColumns[sKey].ctxCanvas.canvas.height = 320;
        
        var arrDataset = [];
        for (var nIdx in g_clsDatasetFair.arrOvColumns[sKey].arrOvData)
        {        
            arrDataset.push( {
                data: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdx].arrLgdVal,
                backgroundColor: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdx].arrLgdClr
            } );
        }
        var config =
        {
            type: 'bar',
            data: {
                labels: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl,
                datasets: arrDataset
            },
            options:
            {
                scales: { xAxes: [ { ticks: { autoSkip: false, } } ], yAxes: [ { ticks: { beginAtZero: true, } } ] },
                responsive: false, 
                maintainAspectRatio: false,
            }
        };
        g_clsDatasetFair.arrOvColumns[sKey].ptrChart = new Chart(g_clsDatasetFair.arrOvColumns[sKey].ctxCanvas, config);
    }
}
