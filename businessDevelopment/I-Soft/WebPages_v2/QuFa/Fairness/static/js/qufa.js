
var arrBtnText = [];
var arrColVal = ['posted_speed_limit', 'weather_condition', 'lighting_condition', 'first_crash_type', 'roadway_surface_cond', 'damage'];

$(document).ready(function()
{
    function csrfSafeMethod(method) { return (/^(GET|HEAD|OPTIONS|TRACE)$/.test(method)); }
    $.ajaxSetup({beforeSend:function(xhr, settings){if(!csrfSafeMethod(settings.type) && !this.crossDomain){xhr.setRequestHeader("X-CSRFToken", csrfTtoken);}}});

    Chart.defaults.global.legend.display = false;
    
    arrBtnText.push( $('#idBtnLoad').html() );
    arrBtnText.push( $('#idBtnRun').html() );
    
    $('.running').prop("disabled",true).addClass('disabled');

    $('#idUlDefColumns').empty();
    
    var sLI = "";
    var chA = 'A'.charCodeAt(0);
    for (var nIdx in arrColVal)
    {
        var sCh = String.fromCharCode(chA);
        sLI = "<li data-id='" + sCh + "'><span style='display:inline-block;width:50px;padding-left:30px;'>" + sCh
            + ":</span><input type='text' id='idTxtCol" + sCh + "' style='width:200px;' value='" + arrColVal[nIdx] + "' /></li>";
        $('#idUlDefColumns').append(sLI);
        chA++;
    }

    $('#idBtnAdd').on('click', function()
    {
        if ( chA > 90 ) return;
        var sCh = String.fromCharCode(chA);
        sLI = "<li data-id='" + sCh + "'><span style='display:inline-block;width:50px;padding-left:30px;'>" + sCh
            + ":</span><input type='text' id='idTxtCol" + sCh + "' style='width:200px;' /></li>";
        $('#idUlDefColumns').append(sLI);
        chA++;
    });
    $('#idBtnDel').on('click', function()
    {
        $('#idUlDefColumns').children().each(function()
        {
            var sCh = String.fromCharCode(chA-1);
            var sID = $(this).attr('data-id');
            if ( sID == sCh )
            {
                $(this).remove();
                chA--;
                return;
            }
        });
    });
    
    $(".sticky-header").floatThead({position:'fixed', top:60, zIndex:100});

    $("#idDivStep0").hide();
    $("#idDivStep1").hide();
    $("#idDivStep2").hide();
    $("#idDivStep3").hide();
});
$(document).on('click', 'a[href*=\\#]', function(event)
{
    event.preventDefault();
    $('html, body').animate(
    {
        scrollTop: $(this.hash).offset().top
    }, 500);
});

var setupComplete = false;
window.addEventListener('WebComponentsReady', function()
{
    if (setupComplete)
    {
        return;
    }
    setupComplete = true;
    var link = document.createElement("link");
    link.rel = "import";
    link.href = sFilePath0;
    link.onload= function()
    {
        var dive = document.createElement("facets-dive");
        dive.id = "fdelem";
        $(".facets-dive-demo")[0].appendChild(dive);
        setupVis();
    };
    document.head.appendChild(link);
});
function setupVis()
{
    var whendone = function(datasetsList)
    {
        //fnMakeOverView(datasetsList);
        fnPrepareOverView();

        var dive = $("#fdelem")[0];
        if (datasetsList.length < 2)
        {
            dive.data = datasetsList[0].data;
        }
        else
        {
            var newFeatureForDive = 'csv-source';
            // var columns = datasetsList.length > 0 ? datasetsList[0].data.columns : [];
            // while (columns.indexOf(newFeatureForDive) > -1)
            // {
            //     newFeatureForDive = newFeatureForDive + newFeatureForDive;
            // }
            datasetsList.forEach(function(dataset)
            {
                dataset.data.rows.forEach(function(datapoint)
                {
                    datapoint[newFeatureForDive] = dataset.name;
                });
            });
            var alldata = datasetsList.reduce(function(a, b)
            {
                return a.concat(b.data.rows);
            }, []);
            dive.data = alldata;
            dive.colorBy = newFeatureForDive;
        }

        return new Promise(function(resolve, reject){ return resolve(true); });
    }
    var fileworker = new Worker(sFilePath1);
    fileworker.onmessage = function(e)
    {
        var datasetsList = e.data;
        whendone(datasetsList);
    };
    fileworker.onerror = function(e)
    {
        console.error('ERROR: Line ', e.lineno, ' in ', e.filename, ': ', e.message);
    };
    var readFileAsync = function()
    {
        var objParam = { files: arrFiles, columns: arrColVal };
        fileworker.postMessage(objParam);
    }
    
    var arrFiles = [];
    var handleFileSelect0 = function(e)
    {
        if ( !e.target.files ) return;
        
        arrFiles[0] = e.target.files[0];

        $("#idFileName0").val(arrFiles[0].name);
    }
    $("#fileupload0")[0].addEventListener('change', handleFileSelect0, false);

    var handleFileSelect1 = function(e)
    {
        if ( !e.target.files ) return;
        
        arrFiles[1] = e.target.files[0];

        $("#idFileName1").val(arrFiles[1].name);
    }
    $("#fileupload1")[0].addEventListener('change', handleFileSelect1, false);
    
    $('#idBtnLoad').on('click', function()
    {
        if ( arrFiles.length > 0 && arrFiles[0] != undefined )
        {
            while (arrColVal.length > 0) { arrColVal.pop(); }
            $('#idUlDefColumns').children().each(function()
            {
                var sCh = $(this).attr('data-id');
                if ( $('#idTxtCol'+sCh).val().length > 0 )
                {
                    arrColVal.push( $('#idTxtCol'+sCh).val() );
                }
            });
            if ( arrColVal.length < 1 )
            {
                alert('Columns is not defined!');
                return;
            }
    
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            $('#idBtnLoad').html("<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>").addClass('fileupload-processing');
            
            $("#idDivStep0").hide();
            $("#idDivStep1").hide();
            $("#idDivStep2").hide();
            $("#idDivStep3").hide();
            
            $('#idDivMetric').html('');
            $('#idDivGraph').html('');
            
            //readFileAsync();
        
            let formData = new FormData();
            formData.append('csrfmiddlewaretoken', csrfTtoken);
            formData.append('columns[]', JSON.stringify(arrColVal));
            formData.append('file0', arrFiles[0]);
            formData.append('file1', arrFiles[1]);
            $.ajax(
            {
                url: "upload/",
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: async function(res)
                {
                    var res = JSON.parse(res);
                    if ( res.success );
                    {
                        await whendone(res.list);
        
                        $('.loading').prop("disabled",false).removeClass('disabled');
                        $('.running').prop("disabled",false).removeClass('disabled');
                        $('#idBtnLoad').html(arrBtnText[0]).removeClass('fileupload-processing');
                                                
                        $("#idDivStep0").show();
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
}

const arrBarColor = [ 'rgba(170, 200, 255, 0.9)', 'rgba(255, 170, 200, 0.9)' ];
var clsDatasetFair = null;

function fnPrepareOverView()
{
    $('#idTblNumeric tbody').empty();
    $('#idTblCategor tbody').empty();
    
    $.ajax(
    {
        url: "overview/",
        type: 'POST',
        processData: false,
        success: function(res)
        {
            clsDatasetFair = JSON.parse(res);

            var nFileCnt = clsDatasetFair.objRawData.length;
            for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
            {
                // Make Raw Data List        
                $('#idTblRawData'+nIdxF.toString()+' thead').empty();      
                var strContents = "<tr><th class='text-center fixed_th td-sm' style='width:3rem'>NO</th>";
                for (var nIdx in arrColVal)
                {
                    strContents += "<th class='text-center fixed_th td-sm'>" + arrColVal[nIdx] + "</th>";
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
    
            fnCreateChart();
            fnPrepareProc();
        }
    })
}
function fnPrepareProc()
{
    var regExp = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\ '\"\\(\=]/gi;

    var sTag = '';
    var sVal = '';
    var sCID = '';

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

    // for test
    $('#idSelCol4Criteria').val(arrColVal[5]).trigger('change');
    $('#idSelCriterColLbl').val('>1500').trigger('change');
    $('#idSelCol4HashBukt').val(arrColVal[3]).trigger('change');
    
    $('#idBtnGoStep1').on('click', function()
    {
        $('.step0').prop("disabled",true).addClass('disabled');
        
        let aSelKey = [$( "#idSelCol4Criteria option:selected" ).text(), $( "#idSelCol4HashBukt option:selected" ).text()];
        
        let sTag = '';
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
        // for test
        $('#idUlList' + arrColVal[1].replace(regExp,'') + ' li').each(function()
        {
            sCID = $(this).attr('data-id');
            if ( !(sCID == 'weathercondition_CLEAR' || sCID == 'weathercondition_RAIN') )
                $('#idChkVal_'+sCID).prop('checked', false);
        });

        $("#idDivStep1").show();
    });
    $('#idBtnGoStep2').on('click', function()
    {
        $('.step1').prop("disabled",true).addClass('disabled');

        let aSelKey = [$( "#idSelCol4Criteria option:selected" ).text(), $( "#idSelCol4HashBukt option:selected" ).text()];
        
        let sTag = '';
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

            $('#idSelParam08').empty();
            for (var nIdx in clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl)
            {
                sVal = clsDatasetFair.arrOvColumns[sSelKey].arrOvData[0].arrLgdLbl[nIdx];
                sTag = "<option value='" + sVal + "'>" + sVal + "</option>";
                $('#idSelParam08').append(sTag);
            }
        });

        sVal = $("#idSelParam07 option:first").val();
        $('#idSelParam07').val(sVal).trigger('change');
        
        // for test
        $('#idSelParam07').val(arrColVal[1]).trigger('change');
        $('#idSelParam08').val('RAIN').trigger('change');

        $("#idDivStep3").show();
    });
    $('#idBtnRun').on('click', function()
    {
        if ( clsDatasetFair != null )
        {    
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            $('#idBtnRun').html("<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>").addClass('fileupload-processing');
            
            let objData = {};
            
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
            objData['Parameters'].push( $( "#idSelParam08 option:selected" ).val() );
            objData['Parameters'].push( $( "#idTxtParam09" ).val() );
            objData['Parameters'].push( $( "#idTxtParam0A" ).val() );

            $.ajax(
            {
                url: "indicator/",
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'text',
                data: JSON.stringify(objData),
                processData: false,
                success: function(res)
                {
                    let json_res = JSON.parse(JSON.parse(res));
                    let sMetric = '';
                    for (let nIdx in json_res.Metric)
                    {
                       for (let sKey in json_res.Metric[nIdx])
                        {
                            sMetric += '<p>' + sKey + ': ' + json_res.Metric[nIdx][sKey] + '</p>';
                        }
                    }
                    $('#idDivMetric').html(sMetric);
                    $('#idDivGraph').html(json_res.ImgTag);
            
                    $('.loading').prop("disabled",false).removeClass('disabled');
                    $('.running').prop("disabled",false).removeClass('disabled');
                    $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
                }
            })
        }
        else
        {
            alert("로딩된 데이터가 없습니다.");            
            $('.loading').prop("disabled",false).removeClass('disabled');
            $('.running').prop("disabled",false).removeClass('disabled');
            $('#idBtnRun').html(arrBtnText[1]).removeClass('fileupload-processing');
            return;
        }
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