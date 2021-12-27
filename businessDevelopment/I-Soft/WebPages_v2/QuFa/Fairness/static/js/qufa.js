
var arrBtnText = [];
var strTagWork = "<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>";
var regExp = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\ '\"\\(\=]/gi;
var varTimer = 0;
var worker = null;
let sKey = '';
// for test
var arrColVal = ['posted_speed_limit', 'weather_condition', 'lighting_condition',
                'first_crash_type', 'roadway_surface_cond', 'crash_type', 'damage'];

$(document).on('click', 'a[href*=\\#]', function(event)
{
    event.preventDefault();
    $('html, body').animate(
    {
        scrollTop: $(this.hash).offset().top
    }, 500);
});
$(document).ready(function()
{
    function csrfSafeMethod(method) { return (/^(GET|HEAD|OPTIONS|TRACE)$/.test(method)); }
    $.ajaxSetup({beforeSend:function(xhr, settings){if(!csrfSafeMethod(settings.type) && !this.crossDomain){xhr.setRequestHeader("X-CSRFToken", csrfTtoken);}}});

    Chart.defaults.global.legend.display = false;
    
    $(".sticky-header").floatThead({position:'fixed', top:60, zIndex:100});
    
    arrBtnText.push( $('#idBtnLoading').html() );
    arrBtnText.push( $('#idBtnRun').html() );
    
    $('.running').prop("disabled",true).addClass('disabled');

    $("#idDivStep0").hide();
    $("#idDivStep1").hide();
    $("#idDivStep2").hide();
    $("#idDivStep3").hide();
    
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
                sKey = res.key;
            }
        }
    })
    
    
    var sTag = '';
    var sVal = '';
    var sCID = '';

    var objFile = null;
    var handleFileSelect = function(e)
    {
        if ( !e.target.files ) return;
        
        objFile = e.target.files[0];

        $("#idFileName").val(objFile.name);
    }
    $("#fileupload")[0].addEventListener('change', handleFileSelect, false);

    $('#idBtnUpload').on('click', function(e)
    {
        e.preventDefault();

        if ( objFile != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('#idBtnLoading').html(strTagWork).addClass('fileupload-processing');
    
            $('#idFileName').val('');
            $('#fileupload').val('');

            let formData = new FormData();
            formData.append('csrfmiddlewaretoken', csrfTtoken);
            formData.append('file', objFile);
            formData.append('key', sKey);
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
                        // for test
                        $('#idAStep1').trigger('click');  

                        fnCheckFileList(true);

                        $('.loading').prop("disabled",false).removeClass('disabled');
                        $('#idBtnLoading').html(arrBtnText[0]).removeClass('fileupload-processing');
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
            fnLoadFile(sFileName);
        }
        else
        {
            alert("Train 파일이 지정되지 않았습니다.");
            return;
        }
    });
    $('#idBtnGoStep1').on('click', function()
    {
        $('.step0').prop("disabled",true).addClass('disabled');
        
        let aSelKey = [$( "#idSelCol4Criteria option:selected" ).text(), $( "#idSelCol4HashBukt option:selected" ).text()];
        
        for (let sKey in clsDatasetFair.arrOvColumns)
        {
            if ( !clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
            {
                sTag += 
                    "<h5 class='font-weight-bold text-left text-secondary'><div class='row'>" +
                    "<div class='col-1'><i class='fas fa-angle-double-right'></i></div>" +
                    "<div class='col-2' style='text-align:right;'>Vocabulary Column:</div>" +
                    "<div class='col-3'>" +
                    "<input type='text' id='idTxtCol" + sKey.replace(regExp,'') + "' class='running step1' style='width:100%;height:28px;' value='" + sKey + "' readonly />" +
                    "</div>" +
                    "<div class='col-2' style='text-align:right;'>Vocabulary List:</div>" +
                    "<div class='col-3'>" +
                    "<ul id='idUlList" + sKey.replace(regExp,'') + "' style='width:100%;'>" +
                    "</ul></div><div class='col-1'></div></div></h5>";
            }
        }
        $('#idDivItem1').html(sTag);
        
        for (let sKey in clsDatasetFair.arrOvColumns)
        {
            if ( !clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
            {
                for (let nIdx in clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl)
                {
                    sVal = clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl[nIdx];
                    sCID = sKey.replace(regExp,'') + "_" + sVal.replace(regExp,'');
                    sTag = "<li data-id='"+ sCID +"'>" +
                        "<span style='display:inline-block;width:300px;'><input type='checkbox' id='idChkVal_"+sCID+"' class='running step1' checked />" +
                        "<label for='idChkVal_"+sCID+"'>&nbsp;"+sVal+"</label></span>" +
                        "<input type='hidden' id='idTxtVal_"+sCID+"' value='"+sVal+"' /></li>";
                    $('#idUlList'+sKey.replace(regExp,'')).append(sTag);
                }
            }
        }

        $("#idDivStep1").show();
    });
    $('#idBtnGoStep2').on('click', function()
    {
        $('.step1').prop("disabled",true).addClass('disabled');

        let aSelKey = [$( "#idSelCol4Criteria option:selected" ).text(), $( "#idSelCol4HashBukt option:selected" ).text()];
        
        for (let sKey in clsDatasetFair.arrOvColumns)
        {
            if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
            {
                sTag += 
                    "<h5 class='font-weight-bold text-left text-secondary'><div class='row'>" +
                    "<div class='col-1'><i class='fas fa-angle-double-right'></i></div>" +
                    "<div class='col-2' style='text-align:right;'>Numeric Column:</div>" +
                    "<div class='col-3'>" +
                    "<input type='text' id='idTxtCol" + sKey.replace(regExp,'') + "' class='running step2' style='width:100%;height:28px;' value='" + sKey + "' readonly />" +
                    "</div>" +
                    "<div class='col-2' style='text-align:right;'>Boundaries:</div>" +
                    "<div class='col-3'>" +
                    "<input type='text' id='idTxtList" + sKey.replace(regExp,'') + "' class='running step2' style='width:100%;height:28px;' value='10,20,30,40,50,60,70,80' />" +
                    "</ul></div><div class='col-1'></div></div></h5>";
            }
        }
        $('#idDivItem2').html(sTag);

        $("#idDivStep2").show();
    });
    $('#idBtnGoStep3').on('click', function()
    {
        $('.step2').prop("disabled",true).addClass('disabled');
        
        $('#idSelParam07').empty();

        let aSelKey = [$( "#idSelCol4Criteria option:selected" ).text(), $( "#idSelCol4HashBukt option:selected" ).text()];
        
        let sTag = '';
        for (var sKey in clsDatasetFair.arrOvColumns)
        {
            if ( !clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && !aSelKey.includes(sKey) )
            {
                sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
                $('#idSelParam07').append(sTag);
            }
        }
        $('#idSelParam07').on('change', function()
        {
            var sSelKey = $( "#idSelParam07 option:selected" ).text();

            sVal = '';
            $('#idUlParam08').empty();
            for (var nIdx in clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl)
            {
                sVal = clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl[nIdx];
                sTag = "<li>" + sVal + "</li>";
                $('#idUlParam08').append(sTag);
            }
        });

        sVal = $("#idSelParam07 option:first").val();
        $('#idSelParam07').val(sVal).trigger('change');
        
        // for test
        $('#idSelParam07').val(arrColVal[5]).trigger('change');

        $("#idDivStep3").show();
    });
    $('#idBtnRun').on('click', function(e)
    {
        e.preventDefault();

        if ( acceessableCount <= 0 ) return;
        acceessableCount = acceessableCount - 1;

        if ( clsDatasetFair != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            $('#idBtnRun').html(strTagWork).addClass('fileupload-processing');
            
            let objData = {};
            objData['key'] = sKey;
            objData['csrfTtoken'] = csrfTtoken;
            
            let sCriteria = $( "#idSelCol4Criteria option:selected" ).text();
            let sCriteLab = $( "#idSelCriterColLbl option:selected" ).text();
            let sHashBukt = $( "#idSelCol4HashBukt option:selected" ).text();
            let sHashSize = $( "#idTxtHashBkSize" ).val();
            
            objData['CriteriaCol'] = sCriteria;
            objData['CriteriaLab'] = sCriteLab;
            objData['HashBuktCol'] = sHashBukt;
            objData['HashBuktSiz'] = sHashSize;
            objData['Categorfeatures'] = [];
            objData['Numericfeatures'] = [];

            let aSelKey = [sCriteria, sHashBukt];
            for (let sKey in clsDatasetFair.arrOvColumns)
            {
                if ( !aSelKey.includes(sKey) )
                {
                    let objItem = {};
                    objItem['ColName'] = sKey;
                    if ( !clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric )
                    {
                        let arrVal = [];
                        $('#idUlList' + sKey.replace(regExp,'') + ' li').each(function()
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
                            $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
                            return;
                        }
                        objItem['VocList'] = arrVal.sort();
                        objData['Categorfeatures'].push(objItem);
                    }
                    else
                    {
                        let list = $('#idTxtList'+sKey.replace(regExp,'')).val();
                        let arrVal = (new Function("return [" + list + "];")());
                        if ( !arrVal.length )
                        {
                            alert("Numeric Feature의 Boundaries List가 비었습니다.");            
                            $('.loading').prop("disabled",false).removeClass('disabled');
                            $('.running').prop("disabled",false).removeClass('disabled');
                            $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
                            return;
                        }                      
                        objItem['BndList'] = arrVal.sort(function(a, b){return a - b});
                        objData['Numericfeatures'].push(objItem);
                    }
                }
            }

            objData['Parameters'] = [];
            objData['Parameters'].push( $( "#idTxtParam00" ).val() );
            objData['Parameters'].push( $( "#idTxtParam01" ).val() );
            objData['Parameters'].push( $( "#idTxtParam02" ).val() );
            objData['Parameters'].push( $( "#idTxtParam03" ).val() );
            objData['Parameters'].push( $( "#idTxtParam04" ).val() );
            objData['Parameters'].push( $( "#idTxtParam05" ).val() );
            objData['Parameters'].push( $( "#idTxtParam06" ).val() );
            objData['Parameters'].push( $( "#idSelParam07 option:selected" ).val() );
            let arrVal = [];
            $('#idUlParam08 li').each(function()
            {
                var sVal = $(this).text();
                arrVal.push(sVal);
            });
            objData['Parameters'].push( arrVal.sort(function(a, b){return a - b}) );
            objData['Parameters'].push( $( "#idTxtParam09" ).val() );
            objData['Parameters'].push( $( "#idTxtParam0A" ).val() );

            worker.postMessage(objData);
        }
        else
        {
            alert("로딩된 데이터가 없습니다.");

            $('.loading').prop("disabled",false).removeClass('disabled');
            $('.running').prop("disabled",false).removeClass('disabled');
            $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
            
            return
        }
        acceessableCount = acceessableCount +1; 
    });
    
    $('#idAStep0').trigger('click');
    
    fnCheckFileList();
    // var id = window.setTimeout(function() {}, 0);
    // while (id--)
    // {
    //     window.clearTimeout(id);
    // }
    // varTimer = setInterval(fnCheckFileList, 10000);

    worker = new Worker(sFilePath);
    worker.onmessage = function(e)
    {
        $('#idAStep4').trigger('click');

        let json_res = JSON.parse(JSON.parse(e.data));
        if ( !json_res.success )
        {
            alert( json_res.message );
            return;
        }
        
        if ( g_bOriginData )
        {
            let aTPR = [];
            for (let nGrp in json_res.Metric)
            {
                for (let nIdx in json_res.Metric[nGrp])
                {
                    let sMetric = '';
                    for (let sKey in json_res.Metric[nGrp][nIdx])
                    {
                        aTPR.push(json_res.Metric[nGrp][nIdx][sKey])
                        sMetric += '<p>' + sKey + ': ' + json_res.Metric[nGrp][nIdx][sKey] + '</p>';
                    }
                    $('#idDivMetricA'+nGrp).html(sMetric);
                }
            }
            $('#idDivGraphA0').html(json_res.ImgTag[0]);
            $('#idDivGraphA1').html(json_res.ImgTag[1]);
    
            // var url = "http://127.0.0.1:5555/api/fairness/tpr?csv=" + json_res.filename[0] + "&tpra=" + aTPR[0] + "&tprb=" + aTPR[2]
            // var url = "http://164.125.37.214:5555/api/fairness/tpr?csv=210719_fairness_test_origin chicago crashes.csv&tpra=0.50&tprb=0.163"
            // var xhttp = new XMLHttpRequest();
            // xhttp.onreadystatechange = function()
            // {
            //     if (this.readyState == XMLHttpRequest.DONE && this.status == 200)
            //     {
            //         var res = this.responseText;
            //     }
            // };
            // xhttp.open("GET", url);
            // xhttp.send();
            
            var url = "http://164.125.37.214:5555/api/fairness/tpr"
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
                        $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
                        
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
            //fnLoadFile("train_after.csv", false);
        }
        else
        {
            let aTPR = [];
            for (let nGrp in json_res.Metric)
            {
                for (let nIdx in json_res.Metric[nGrp])
                {
                    let sMetric = '';
                    for (let sKey in json_res.Metric[nGrp][nIdx])
                    {
                        aTPR.push(json_res.Metric[nGrp][nIdx][sKey])
                        sMetric += '<p>' + sKey + ': ' + json_res.Metric[nGrp][nIdx][sKey] + '</p>';
                    }
                    $('#idDivMetricB'+nGrp).html(sMetric);
                }
            }
            $('#idDivGraphB0').html(json_res.ImgTag[0]);
            $('#idDivGraphB1').html(json_res.ImgTag[1]);

            $.ajax(
            {
                url: 'getresult/',
                type: 'POST',
                data: JSON.stringify({'key': sKey}),
                dataType: 'JSON',
                success: function(res)
                {
                    var res = JSON.parse(res);
                    var nPercentage = res.result.rate * 100.0;
                    var sTag = '<p style="font-size:48px;">보정 성능 : ' + nPercentage.toFixed(3) + ' % ' + '</p>';
                    $('#idDivPerform').html(sTag);
                }
            })

            g_bProcDone = true;

            // for test
            $('.loading').prop("disabled",false).removeClass('disabled');
            $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
        }
    };
    worker.onerror = function(e)
    {
        alert("Error : " + e.message + " (" + e.filename + ":" + e.lineno + ")");
    };
});

var g_bAftersetBegin = true;
var g_bProcDone = true;
var clsDatasetFair = null;
function fnCheckFileList(update = false)
{
    if ( !g_bProcDone ) return;
    
    $.ajax(
    {
        url: "getcount/",
        type: 'POST',
        data: JSON.stringify({'key': sKey}),
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
                        data: JSON.stringify({'key': sKey}),
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
        $('#idBtnLoading').html(strTagWork).addClass('fileupload-processing');

        $("#idDivStep0").hide();
        $("#idDivStep1").hide();
        $("#idDivStep2").hide();
        $("#idDivStep3").hide();

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
    
    clsDatasetFair = null;

    $.ajax(
    {
        url: "loading/",
        type: 'POST',
        data: JSON.stringify({'key': sKey, 'filename': sFileTRN, 'bOriginData': bOriginData}),
        dataType: 'JSON',
        success: async function(res)
        {
            var res = JSON.parse(res);
            if ( res.success == 'true' )
            {
                if ( bOriginData )
                {
                    fnCheckFileList(true);
                }
                await fnPrepareOverView();

                if ( bOriginData )
                {
                    $('.loading').prop("disabled",false).removeClass('disabled');
                    $('#idBtnLoading').html(arrBtnText[0]).removeClass('fileupload-processing');
                    $('.running').prop("disabled",false).removeClass('disabled');
                }
                await fnPrepareProc();

                // for test
                if ( !g_bProcDone )
                {
                    $('#idBtnGoStep1').trigger('click');
                    $('#idBtnGoStep2').trigger('click');
                    $('#idBtnGoStep3').trigger('click');
                    // if ( bOriginData )
                    // {
                    //     $('#idAStep3').trigger('click');
                    // }
                    $('#idBtnRun').trigger('click');
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
        $('#idTblNumeric tbody').empty();
        $('#idTblCategor tbody').empty();
        
        $.ajax(
        {
            url: "overview/",
            type: 'POST',
            data: JSON.stringify({'key': sKey}),
            dataType: 'JSON',
            success: async function(res)
            {
                var res = JSON.parse(res);
                if ( res.success == 'true' )
                {
                    clsDatasetFair = res.data;

                    var nFileCnt = clsDatasetFair.objRawData.length;
                    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                    {
                        // Make Raw Data List        
                        $('#idTblRawData'+nIdxF.toString()+' thead').empty();      
                        var strContents = "<tr><th class='text-center fixed_th td-sm' style='width:3rem'>NO</th>";
                        for (var nCol in clsDatasetFair.objRawData[nIdxF].columns)
                        {
                            var sKey = clsDatasetFair.objRawData[nIdxF].columns[nCol];
                            strContents += "<th class='text-center fixed_th td-sm'>" + sKey + "</th>";
                        }
                        strContents += "</tr>";
                        $('#idTblRawData'+nIdxF.toString()+' thead').append(strContents);
                
                        $('#idTblRawData'+nIdxF.toString()+' tbody').empty();
                        strContents = '';
                        for (var nRow in clsDatasetFair.objRawData[nIdxF].rows)
                        {
                            var nNum = +nRow;
                            if ( isNaN(nNum) ) continue;
                            if ( nNum >= 5 ) break;
                            
                            strContents += "<tr><td class='text-right td-sm'>" + (nNum+1).toString() + "</td>";
                            for (var nCol in clsDatasetFair.objRawData[nIdxF].columns)
                            {
                                var sKey = clsDatasetFair.objRawData[nIdxF].columns[nCol];
                
                                strContents += "<td class='text-center td-sm'>";
                                strContents += clsDatasetFair.objRawData[nIdxF].rows[nRow][sKey];
                                strContents += '</td>';
                            }
                            strContents += '</tr>';
                        }
                        $('#idTblRawData'+nIdxF.toString()+' tbody').append(strContents);
                
                        // Make Overview
                        for (var nCol in clsDatasetFair.objRawData[nIdxF].columns)
                        {
                            var sKey = clsDatasetFair.objRawData[nIdxF].columns[nCol];
                
                            // Make Chart Data
                            clsDatasetFair.arrOvColumns[sKey].idCanvas = 'idCvsChart' + nCol.toString();
                            clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl = [];
                            clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal = [];
                            clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr = [];
                            
                            for (var sVal in clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr)
                            {
                                var fVal = ( clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].bNumeric ) ? +sVal : sVal;
                                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl.push( fVal );
                                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal.push( +clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sVal] );
                                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr.push( arrBarColor[nIdxF] );
                            }
                        }
                    }
                    // Make Table
                    var sTableItem = '';
                    for (var sKey in clsDatasetFair.arrOvColumns)
                    {
                        if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric )
                        {
                            sTableItem = "<tr style='height:20px;'>"
                                + "<td colspan=7 class='font-weight-bold'>" + sKey + "</td>"
                                + "<td rowspan=" + (nFileCnt+1).toString() + "><div style='padding-left:15px;height:360px;'>"
                                + "<canvas id='" + clsDatasetFair.arrOvColumns[sKey].idCanvas + "' style='width:480px;height:320px;'></canvas>"
                                + "</div></td>"
                                + "</tr>"
                            $('#idTblNumeric').append(sTableItem);
                
                            for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                            {
                                var nRowCnt = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount;
                                var fPercentage = (clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nZeros/nRowCnt)*100;
                                sTableItem = "<tr>"
                                    + "<td class='text-center'>" + nRowCnt + "</td>"
                                    + "<td class='text-center'>" + (Math.round(clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "<td class='text-center'>" + (Math.round(clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fStdDev*100)/100).toFixed(2) + "</td>"
                                    + "<td class='text-center'>" + (Math.round(fPercentage *100)/100).toFixed(2) + "%</td>"
                                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMin + "</td>"
                                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMedian + "</td>"
                                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMax + "</td>"
                                    + "</tr>";
                                $('#idTblNumeric').append(sTableItem);
                            }
                        }
                        else
                        {
                            sTableItem = "<tr style='height:20px;'>"
                                + "<td colspan=5 class='font-weight-bold'>" + sKey + "</td>"
                                + "<td rowspan=" + (nFileCnt+1).toString() + "><div style='padding-left:15px;height:360px;'>"
                                + "<canvas id='" + clsDatasetFair.arrOvColumns[sKey].idCanvas + "' style='width:480px;height:320px;'></canvas>"
                                + "</div></td>"
                                + "</tr>";
                            $('#idTblCategor').append(sTableItem);
                            
                            for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                            {
                                var sMaxKey = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].sMaxUniqueKey;
                                sTableItem = "<tr>"
                                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount + "</td>"
                                    + "<td class='text-center'>" + Object.keys(clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr).length + "</td>"
                                    + "<td class='text-center'>" + sMaxKey + "</td>"
                                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sMaxKey] + "</td>"
                                    + "<td class='text-center'>" + (Math.round(clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "</tr>";
                                $('#idTblCategor').append(sTableItem);
                            }
                        }
                    }
                    
                    // for test
                    $('#idAStep2').trigger('click');  

                    fnCreateChart();

                    return resolve(true);
                }
            }
        })
    });
}
var acceessableCount = 1;
function fnPrepareProc()
{
    return new Promise(function(resolve, reject)
    { 
        var sTag = '';
        var sVal = '';

        $('#idSelCol4Criteria').empty();
        $('#idSelCriterColLbl').empty();
        $('#idSelCol4HashBukt').empty();

        for (var sKey in clsDatasetFair.arrOvColumns)
        {
            sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
            $('#idSelCol4Criteria').append(sTag);
            $('#idSelCol4HashBukt').append(sTag);
        }
        $('#idSelCol4Criteria').on('change', function()
        {
            var sSelKey = $( "#idSelCol4Criteria option:selected" ).text();

            $('#idSelCriterColLbl').empty();
            for (var nIdx in clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl)
            {
                sVal = clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl[nIdx];
                sTag = "<option value='" + sVal + "'>" + sVal + "</option>";
                $('#idSelCriterColLbl').append(sTag);
            }
            $('#idSelCol4HashBukt').empty();
            for (var sKey in clsDatasetFair.arrOvColumns)
            {
                if ( !clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric && sKey != sSelKey )
                {
                    sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
                    $('#idSelCol4HashBukt').append(sTag);
                }
            }
        });

        sVal = $("#idSelCol4Criteria option:first").val();
        $('#idSelCol4Criteria').val(sVal).trigger('change');
                                            
        $("#idDivStep0").show(); 

        // for test
        $('#idSelCol4Criteria').val(arrColVal[6]).trigger('change');
        $('#idSelCriterColLbl').val('>1500').trigger('change');
        $('#idSelCol4HashBukt').val(arrColVal[3]).trigger('change');

        return resolve(true);
    });
}
function zeroPadding(num, size)
{
    if ( num.toString().length >= size ) return num;
    return ( Math.pow(10, size) + Math.floor(num) ).toString().substring(1);
}
function median(numbers)
{
    var median = 0, numsLen = numbers.length;
    numbers.sort();
    if ( numsLen % 2 === 0 ) // even
    {
        median = (numbers[numsLen / 2 - 1] + numbers[numsLen / 2]) / 2;
    }
    else // odd
    {
        median = numbers[(numsLen - 1) / 2];
    }        
    return median;
}
function fnCreateChart()
{
    for (var sKey in clsDatasetFair.arrOvColumns)
    {        
        if ( clsDatasetFair.arrOvColumns[sKey].ptrChart != null ) clsDatasetFair.arrOvColumns[sKey].ptrChart.destroy();

        var idCanvas = clsDatasetFair.arrOvColumns[sKey].idCanvas;
        clsDatasetFair.arrOvColumns[sKey].ctxCanvas = document.getElementById(idCanvas).getContext('2d');
        clsDatasetFair.arrOvColumns[sKey].ctxCanvas.canvas.width = 480;
        clsDatasetFair.arrOvColumns[sKey].ctxCanvas.canvas.height = 320;
        
        var arrDataset = [];
        for (var nIdx in clsDatasetFair.arrOvColumns[sKey].arrOvData)
        {        
            arrDataset.push( {
                data: clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdx].arrLgdVal,
                backgroundColor: clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdx].arrLgdClr
            } );
        }
        var config =
        {
            type: 'bar',
            data: {
                labels: clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl,
                datasets: arrDataset
            },
            options:
            {
                scales: { xAxes: [ { ticks: { autoSkip: false, } } ], yAxes: [ { ticks: { beginAtZero: true, } } ] },
                responsive: false, 
                maintainAspectRatio: false,
                // events: false,
                // tooltips: { enabled: false },
                // hover: { animationDuration: 0 },
                // animation:
                // {
                //     duration: 1,
                //     onComplete: function () 
                //     {
                //         var chartInstance = this.chart, ctx = chartInstance.ctx;
                //         ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, Chart.defaults.global.defaultFontStyle, Chart.defaults.global.defaultFontFamily);
                //         ctx.textAlign = 'center';
                //         ctx.textBaseline = 'bottom';
                //         this.data.datasets.forEach(function (dataset, i)
                //         {
                //             var meta = chartInstance.controller.getDatasetMeta(i);
                //             meta.data.forEach(function (bar, index)
                //             {
                //                 var data = dataset.data[index];                            
                //                 ctx.fillText(data, bar._model.x, bar._model.y - 5);
                //             });
                //         });
                //     }
                // }
            }
        };
        clsDatasetFair.arrOvColumns[sKey].ptrChart = new Chart(clsDatasetFair.arrOvColumns[sKey].ctxCanvas, config);
    }
}
