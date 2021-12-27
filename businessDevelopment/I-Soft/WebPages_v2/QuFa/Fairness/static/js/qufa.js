// global variable
let g_arrBtnText = [];
let g_strTagWork = "<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>";
let g_strTagBusy = "<i class='fas fa-spinner fa-pulse' style='font-size:100px;color:#CCC;'></i>";
let g_regExp = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\ '\"\\(\=]/gi;
let g_wkrProcSnB = null;
let g_wkrProcTPR = null;
let g_sAccessKey = '';
const g_arrMLTask = 
[
    { key: 'binary' , name: '이진 분류', parity: 
    [
        { key: 'equaloppo' , name: '균등 기회' },
        { key: 'equalodds' , name: '균등 승률' },
        { key: 'dmgparity' , name: '인구 통계 패리티' },
    ] },
    { key: 'regres' , name: '회귀 분류', parity:
    [
        { key: 'dmgparity' , name: '인구 통계 패리티' },
        { key: 'bndgrplos' , name: '제한된 그룹 손실' },
    ] },
];
let g_oMLTaskIdx = { mltask: 0, parity: 0 };
let g_bManualRun = false;
let g_bOriginData = true;
let g_bProcDone = true;
let g_clsDatasetFair = null;
let g_arrDataSunburst = null;
const g_cD3scaleOrd = d3.scaleOrdinal(d3.schemePaired);//d3.schemeCategory10);//
const g_arrMLModel = 
[
    { key: 'gi' , name: 'Tensorflow Keras' },
    { key: 'lr' , name: 'Logistic Regression' },
    { key: 'sdg', name: 'SGD Classifier' },
    { key: 'svm', name: 'Linear SVC' }
];
let g_bRunTsneUmap = false;
// for preset
let bTrafficData = true;
const arrSubStrID = ['traffic','health'];
const arrPreset0 = [6,0];
let arrColHdr0 = ['posted_speed_limit', 'weather_condition', 'lighting_condition',
                  'first_crash_type', 'roadway_surface_cond', 'crash_type', 'damage'];
let arrColInf0 = [
    {col: 'posted_speed_limit', name: '사고당시 속도',
        item: [{key:'0',name:'40km 이하',eng:'Less than 40km'},{key:'1',name:'40km 초과',eng:'Over 40km'}]},
    {col: 'weather_condition', name: '사고당시 날씨상태',
        item: [{key:'0',name:'CLEAR'},{key:'1',name:'CLOUDY/OVERCAST'},{key:'2',name:'FOG/SMOKE/HAZE'},
               {key:'3',name:'OTHER'},{key:'4',name:'RAIN'},{key:'5',name:'SEVERE CROSS WIND GATE'},
               {key:'6',name:'SLEET/HAIL'},{key:'7',name:'SNOW'},{key:'8',name:'UNKNOWN'}]},
    {col: 'lighting_condition', name: '사고당시 조명상태',
        item: [{key:'0',name:'DARKNESS'},{key:'1',name:'DARKNESS/LIGHTED ROAD'},{key:'2',name:'DAWN'},
               {key:'3',name:'DAYLIGHT'},{key:'4',name:'DUSK'},{key:'5',name:'UNKNOWN'}]},
    {col: 'first_crash_type', name: '최초 충돌 유형',
        item: [{key:'0',name:'ANGLE'},{key:'1',name:'ANIMAL'},{key:'2',name:'FIXED OBJECT'},{key:'3',name:'HEAD ON'},
               {key:'4',name:'OTHER NONCOLLISION'},{key:'5',name:'OTHER OBJECT'},{key:'6',name:'OVERTURNED'},
               {key:'7',name:'PARKED MOTOR VEHICLE'},{key:'8',name:'PEDELCYCLIST'},{key:'9',name:'PEDESTRIAN'},
               {key:'10',name:'REAR END'},{key:'11',name:'SIDESWIPE OPPOSITE DIRECTION'},
               {key:'12',name:'SIDESWIPE SAME DIRECTION'},{key:'13',name:'TRAIN'},{key:'14',name:'TURNING'}]},
    {col: 'roadway_surface_cond', name: '사고지역 노면상태',
        item: [{key:'0',name:'DRY'},{key:'1',name:'ICE'},{key:'2',name:'OTHER'},{key:'3',name:'SAND/MUD/DIRT'},
               {key:'4',name:'SNOW OR SLUSH'},{key:'5',name:'UNKNOWN'},{key:'6',name:'WET'}]},
    {col: 'crash_type', name: '부상유무',
        item: [{key:'0',name:'있음'},{key:'1',name:'없음'}]},
    {col: 'damage', name: '사고 피해 수리비',
        item: [{key:'0',name:'$1,500 이하',eng:'Less than $1,500'},{key:'1',name:'$1,500 초과',eng:'Over $1,500'}]}];
const arrPreset1 = [0,2];
let arrColHdr1 = ['sex', 'age', 'cva', 'fcvayn', 'packyear', 'sd_idr2', 'exerfq'];
let arrColInf1 = [
    {col: 'sex', name: '성별', item: [{key:'0',name:'남성',eng:'Male'},{key:'1',name:'여성',eng:'Female'}]},
    {col: 'age', name: '연령 [ _ 세]', item: []},
    {col: 'cva', name: '과거력 뇌졸증', item: [{key:'0',name:'없음'},{key:'1',name:'있음'}]},
    {col: 'fcvayn', name: '가족력 뇌졸증(중풍)', item: [{key:'0',name:'없음'},{key:'1',name:'있음'}]},
    {col: 'packyear', name: '하루흡연량(갑) x 흡연기간(년) [ _ 갑]', item: []},
    {col: 'sd_idr2', name: '일주일 간 음주 빈도 [ _ 회]', item: []},
    {col: 'exerfq', name: '일주일 간 운동 빈도 [ _ 회]', item: []}];
let jsonResult = {};

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
    
    g_arrBtnText.push( $('#idBtnLoading').html() );
    g_arrBtnText.push( $('#idBtnRunSnB').html() );
    g_arrBtnText.push( $('#idBtnRunTPR').html() );
    
    $('#idBtnZoomA').prop("disabled",true).addClass('disabled');
    $('#idBtnZoomB').prop("disabled",true).addClass('disabled');
    $('.running').prop("disabled",true).addClass('disabled');

    $('#idDivParamMLT0').hide();
    $('#idDivParamMLT1').hide();
    $('#idDivParamGi').hide();
    
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

    var sHtml = "";
    for (var nIdx = 0; nIdx < g_arrMLTask.length; nIdx++)
    {
        sHtml += "<span style='display:inline-block;width:200px;'>";
        sHtml += "<input class='loading' type='radio' id='idRad"+g_arrMLTask[nIdx].key
        sHtml += "' name='MLTask' value='"+g_arrMLTask[nIdx].key+"' />";
        sHtml += "<label for='idRad"+g_arrMLTask[nIdx].key+"'>&nbsp;"+g_arrMLTask[nIdx].name+"</label>";
        sHtml += "</span>";
    }
    $('#idDivMLTask').html(sHtml);

    $('input[name="MLTask"]').each(function()
    {
        var sValue = $(this).val();
        var nArrIdxM = g_arrMLTask.findIndex(x => x.key === sValue);
        $('.cls'+g_arrMLTask[nArrIdxM].key).hide();
    });
    $('input[name="MLTask"]').change(function()
    {
        $('input[name="MLTask"]').each(function()
        {
            var bChecked = $(this).prop('checked');
            var $label = $(this).next();
            var sValue = $(this).val();
            var nArrIdxM = g_arrMLTask.findIndex(x => x.key === sValue);
            if ( nArrIdxM < 0 )
            {
                alert("invalid key!");
                return;
            }
            if ( bChecked )
            {
                $label.css('font-weight', 'bold');

                g_oMLTaskIdx.mltask = nArrIdxM;
                
                var aParity = g_arrMLTask[nArrIdxM].parity;
                var sHtml = "";
                for (var nIdx = 0; nIdx < aParity.length; nIdx++)
                {
                    sHtml += "<span style='display:inline-block;width:200px;'>";
                    sHtml += "<input class='loading' type='radio' id='idRad"+aParity[nIdx].key
                    sHtml += "' name='Parity' value='"+aParity[nIdx].key+"' />";
                    sHtml += "<label for='idRad"+aParity[nIdx].key+"'>&nbsp;"+aParity[nIdx].name+"</label>";
                    sHtml += "</span>";
                }
                $('#idDivParity').html(sHtml);
                
                sTag = '#idRad'+aParity[0].key;
                $('input[name="Parity"]').change(function()
                {
                    $('input[name="MLTask"]').each(function()
                    {
                        var sValue = $(this).val();
                        var nArrIdxM = g_arrMLTask.findIndex(x => x.key === sValue);
                        $('.cls'+g_arrMLTask[nArrIdxM].key).hide();
                    });
                    $('input[name="Parity"]').each(function()
                    {
                        var aMLTask = g_arrMLTask[g_oMLTaskIdx.mltask];

                        var bChecked = $(this).prop('checked');
                        var $label = $(this).next();
                        var sValue = $(this).val();
                        var nArrIdxS = aMLTask.parity.findIndex(x => x.key === sValue);
                        if ( nArrIdxS < 0 )
                        {
                            alert("invalid key!");
                            return;
                        }
                        if ( bChecked )
                        {
                            $label.css('font-weight', 'bold');

                            g_oMLTaskIdx.parity = nArrIdxS;

                            $('.cls'+g_arrMLTask[nArrIdxM].key+aMLTask.parity[nArrIdxS].key).show();
                        }
                        else
                        {
                            $label.css('font-weight', '');
                            
                            $('.cls'+g_arrMLTask[nArrIdxM].key+aMLTask.parity[nArrIdxS].key).hide();
                        }
                        
                        if ( aMLTask.key === 'regres' )
                        {
                            $(this).attr("disabled", "true");
                        }
                    });
                });
                $(sTag).trigger('click');
            }
            else
            {
                $label.css('font-weight', '');
            }
        });
    });
    sTag = '#idRad'+g_arrMLTask[0].key;
    $(sTag).trigger('click');
    sTag = '#idRad'+g_arrMLTask[0].parity[0].key;
    $(sTag).trigger('click');

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
        $('#idBtnRunSnB').html(g_arrBtnText[1] + html);
        $('#idBtnRunTPR').html(g_arrBtnText[2] + html);
        // if ( g_bManualRun && g_bRunTsneUmap )
        // {
        //     g_bRunTsneUmap = false;
        //     $('#idChkRunTsneUmap').prop('checked', g_bRunTsneUmap);
        // }
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
        var aMLTask = g_arrMLTask[g_oMLTaskIdx.mltask];
        if ( aMLTask.key === 'regres' )
        {  
            alert("현재 회귀 분류는 미구현입니다.\r\n이진 분류로 진행하여 주십시오.");
            $('#idAStep0').trigger('click');
            return;
        }

        let sFileName = $( "#idSelFileList option:selected" ).val();
        if ( sFileName != undefined )
        {
            // for preset
            bTrafficData = sFileName.includes(arrSubStrID[0]);
            if ( !bTrafficData )
            {
                if ( !sFileName.includes(arrSubStrID[1]) )
                {
                    alert("지원되지 않는 파일 이름입니다.");
                    return;
                }
            }

            fnLoadFile(sFileName);
        }
        else
        {
            alert("Train 파일이 지정되지 않았습니다.");
            return;
        }
    });
    $('#idBtnRunSnB').on('click', function(e)
    {
        e.preventDefault();

        if ( g_clsDatasetFair != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            $('#idBtnRunSnB').html(g_strTagWork);
        
            var sHeight = ($('#idDivSbChartA').height()/2)-65;
            var sSpace = "<p style='height:"+sHeight.toString()+"px;'></p>";
            if ( g_bOriginData )
            {
                $('#idDivSbChartA').html(sSpace + g_strTagBusy);
                
            }
            else
            {
                $('#idDivSbChartB').html(sSpace + g_strTagBusy);
            }
            
            let objData = {};
            objData['url'] = "../../Fairness/sunburst/";
            objData['key'] = g_sAccessKey;
            objData['csrfTtoken'] = csrfTtoken;
            
            objData['ClassifyCol'] = $('#idSelCol4Classify option:selected').text();
            objData['SubGroupCol'] = $('#idSelCol4SubGroup option:selected').text();
            
            g_wkrProcSnB.postMessage(objData);
        }
        else
        {
            alert("로딩된 데이터가 없습니다.");

            $('.loading').prop("disabled",false).removeClass('disabled');
            $('.running').prop("disabled",false).removeClass('disabled');
            $('#idBtnRunSnB').html(g_arrBtnText[1]);
            
            return
        }
    });
    $('#idBtnRunTPR').on('click', function(e)
    {
        e.preventDefault();

        if ( acceessableCount <= 0 ) return;
        acceessableCount = acceessableCount - 1;

        if ( g_clsDatasetFair != null )
        {
            $('.loading').prop("disabled",true).addClass('disabled');
            $('.running').prop("disabled",true).addClass('disabled');
            var html = (g_bOriginData) ? ' (AFTER)':' (FINISHED)';
            $('#idBtnRunSnB').html(g_arrBtnText[1] + html);
            $('#idBtnRunTPR').html(g_strTagWork);
            
            let objData = {};
            objData['url'] = "../../Fairness/run_alg/";
            objData['key'] = g_sAccessKey;
            objData['csrfTtoken'] = csrfTtoken;
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

            objData['AlgorithmType'] = $('#idSelMLModel option:selected').val();
            if ( objData['AlgorithmType'] == 'gi')
            {
                objData['AlgParameters'] = {};
                objData['AlgParameters']['Parameters'] = [];
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam00" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam01" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam02" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam03" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam04" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam05" ).val() );
                objData['AlgParameters']['Parameters'].push( $( "#idTxtParam06" ).val() );

                objData['AlgParameters']['HashBucketSize'] = $('#idTxtHashBkSize').val();
                objData['AlgParameters']['Categorfeatures'] = [];
                objData['AlgParameters']['Numericfeatures'] = [];

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
                                $('#idBtnRunTPR').html(g_arrBtnText[2]);
                                return;
                            }
                            objItem['VocList'] = arrVal.sort();
                            objData['AlgParameters']['Categorfeatures'].push(objItem);
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
                                $('#idBtnRunTPR').html(g_arrBtnText[2]);
                                return;
                            }
                            objItem['BndList'] = arrVal.sort(function(a, b){return a - b});
                            objData['AlgParameters']['Numericfeatures'].push(objItem);
                        }
                    }
                }
            }
            objData['RunTsneUmap'] = g_bRunTsneUmap ? 'on' : 'off';
            objData['TestsetPath'] = (bTrafficData)?"/csv/traffic/":"/csv/health/"
            objData['TestsetFile'] = (bTrafficData)?"traffic_testset.csv":"health_testset.csv"
            g_wkrProcTPR.postMessage(objData);
        }
        else
        {
            alert("로딩된 데이터가 없습니다.");

            $('.loading').prop("disabled",false).removeClass('disabled');
            $('.running').prop("disabled",false).removeClass('disabled');
            $('#idBtnRunTPR').html(g_arrBtnText[2]);
            
            return
        }
        acceessableCount = acceessableCount +1; 
    });

    $('#idSelMLModel').empty();
    for (var nIdx in g_arrMLModel)
    {
        sTag = "<option value='" + g_arrMLModel[nIdx].key + "'>" + g_arrMLModel[nIdx].name + "</option>";
        $('#idSelMLModel').append(sTag);
    }
    $('#idSelMLModel').on('change', function()
    {
        if ( $(this).val() == 'gi')
        {
            $("#idDivParamGi").show();
            $('#idDivGraphA2').hide();
            $('#idDivGraphB2').hide();
        }
        else
        {
            $("#idDivParamGi").hide();
            $('#idDivGraphA2').show();
            if ( g_bRunTsneUmap )
            {
                $('#idDivGraphA2').show();
                $('#idDivGraphB2').show();
            }
            else
            {
                $('#idDivGraphA2').hide();
                $('#idDivGraphB2').hide();
            }
        }
    });
    $('#idChkRunTsneUmap').prop('checked', g_bRunTsneUmap);
    $('#idChkRunTsneUmap').on('click', function(e)
    {
        g_bRunTsneUmap = $('#idChkRunTsneUmap').prop('checked');
        if ( g_bRunTsneUmap )
        {
            $('#idDivGraphA2').show();
            $('#idDivGraphB2').show();
        }
        else
        {
            $('#idDivGraphA2').hide();
            $('#idDivGraphB2').hide();
        }
    });
    
    fnCheckFileList();
    
    var nWid = $(window).width();
    var nHig = $(window).height();
    var nSize = (nWid<nHig) ? nWid : nHig;
    $("#idDlgHighChart").dialog(
    {
        autoOpen: false,
        modal: true,
        width: nSize,
        height: nSize,
        resizable: false, 
        draggable: false, 
    });
    $("#idDlgOverview").dialog(
    {
        autoOpen: false,
        modal: true,
        width: nSize,
        height: nSize,
        resizable: false, 
        draggable: false, 
    });
    $("#idDlgSunburst").dialog(
    {
        autoOpen: false,
        modal: true,
        width: nSize,
        height: nSize,
        resizable: false, 
        draggable: false, 
    });

    g_wkrProcSnB = new Worker(strWrkPath);
    g_wkrProcSnB.onmessage = function(e)
    {
        $('#idAStep3').trigger('click');

        let json_res = JSON.parse(JSON.parse(e.data));
        if ( !json_res.success )
        {
            alert( json_res.message );
            return;
        }

        arrData = json_res.data;
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
                
                $('#idBtnZoomA').prop("disabled",false).removeClass('disabled');                
                $('#idBtnZoomA').on('click', function(e)
                {
                    if (g_arrDataSunburst.length < 1) return;
                    $("#idDlgSunburst").dialog("open"); 
                    var nW = $('#idDivSbChart').width();
                    var nH = $('#idDivSbChart').height();
                    $('#idDivSbChart').html('');
                    Sunburst().data(g_arrDataSunburst[0])
                    .width(nW)
                    .height(nH)
                    .color((d, parent) => g_cD3scaleOrd(parent ? parent.data.name : null))    
                    .excludeRoot(true)
                    .radiusScaleExponent(1)
                    (document.getElementById('idDivSbChart'));
                });
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
                
                $('#idBtnZoomB').prop("disabled",false).removeClass('disabled');
                $('#idBtnZoomB').on('click', function(e)
                {
                    if (g_arrDataSunburst.length < 2) return;
                    $("#idDlgSunburst").dialog("open"); 
                    var nW = $('#idDivSbChart').width();
                    var nH = $('#idDivSbChart').height();
                    $('#idDivSbChart').html('');
                    Sunburst().data(g_arrDataSunburst[1])
                    .width(nW)
                    .height(nH)
                    .color((d, parent) => g_cD3scaleOrd(parent ? parent.data.name : null))    
                    .excludeRoot(true)
                    .radiusScaleExponent(1)
                    (document.getElementById('idDivSbChart'));
                });
            }

            if ( !g_bProcDone )
            {
                var html = (g_bOriginData) ? ' (BEFORE)':' (AFTER)';
                $('#idBtnRunSnB').html(g_arrBtnText[1] + html);
                
                if ( g_bManualRun )
                {
                    $('.running').prop("disabled",false).removeClass('disabled');
                }
                else
                {
                    $('#idBtnRunTPR').trigger('click');
                }
            }
        }
    };
    g_wkrProcSnB.onerror = function(e)
    {
        alert("Error : " + e.message + " (" + e.filename + ":" + e.lineno + ")");
    };

    g_wkrProcTPR = new Worker(strWrkPath);
    g_wkrProcTPR.onmessage = function(e)
    {
        let json_res = JSON.parse(JSON.parse(e.data));
        if ( !json_res.success )
        {
            alert( json_res.message );
            return;
        }
        if ( g_bOriginData )
        {
            $('#idAStep4').trigger('click');
        }
        else
        {
            $('#idAStep5').trigger('click');
        }
        jsonResult = json_res;
        fnDisplayResult(json_res);
    };
    g_wkrProcTPR.onerror = function(e)
    {
        alert("Error : " + e.message + " (" + e.filename + ":" + e.lineno + ")");
    };
    
    $('#idAStep0').trigger('click');
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
        g_arrDataSunburst = [];
        
        $('.loading').prop("disabled",true).addClass('disabled');
        $('#idBtnLoading').html(g_strTagWork).addClass('fileupload-processing');
        
        $('#idDivOverview0').empty();
        $('#idDivOverview1').empty();
        
        $('#idDivSbChartA').html('');
        $('#idDivSbChartB').html('');
        $('#idBtnZoomA').prop("disabled",true).addClass('disabled');
        $('#idBtnZoomB').prop("disabled",true).addClass('disabled');

        $('#idDivParamMLT0').hide();
        $('#idDivParamMLT1').hide();
        $('#idDivParamGi').hide();

        $('#idDivConfMatA0').html('');
        $('#idDivConfMatA1').html('');
        $('#idDivGraphA0').html('');
        $('#idDivGraphA1').html('');
        $('#idDivConfMatB0').html('');
        $('#idDivConfMatB1').html('');
        $('#idDivGraphB0').html('');
        $('#idDivGraphB1').html('');
        $('#idDivPerform').html('');
    }
    
    g_clsDatasetFair = null;

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
                $('#idBtnLoading').html(g_arrBtnText[0]).removeClass('fileupload-processing');
            
                $('#idAStep2').trigger('click');
                await fnPrepareOverView();

                $('#idAStep3').trigger('click');
                await fnPrepareProc();

                if ( g_bManualRun )
                {
                    $('.running').prop("disabled",false).removeClass('disabled');
                    var html = (bOriginData) ? ' (BEFORE)':' (AFTER)';
                    $('#idBtnRunSnB').html(g_arrBtnText[1] + html);
                    $('#idBtnRunTPR').html(g_arrBtnText[2] + html);
                }
                else
                {
                    $('#idBtnRunSnB').trigger('click');
                }
            }
        }
    })
}

const arrBarColor = [ 'rgba(170, 200, 255, 0.9)', 'rgba(255, 170, 200, 0.9)' ];
const arrThdColor = [ '#AAC8FF', '#FFAAC8' ];

function fnPrepareOverView()
{
    return new Promise(function(resolve, reject)
    {
        var sSpace = "<div style='text-align:center;'><p style='height:20px;'></p>";
        if ( g_bOriginData )
        {
            $('#idDivOverview0').html(sSpace + g_strTagBusy + '</div>');
        }
        else
        {
            $('#idDivOverview1').html(sSpace + g_strTagBusy + '</div>');
        }
        
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
                            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas = 'idCvsChart' + nIdxF.toString() + nCol.toString();
                            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ptrChart = null;
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

                    // Make OV Table
                    var sHtml = [];
                    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                    {
                        var sCtrlID = '#idDivOverview' + nIdxF.toString();
                        $(sCtrlID).empty();

                        sHtml[nIdxF] = "<table class='table' style='width:100%' cellspacing='0'>";
                        for (var sKey in g_clsDatasetFair.arrOvColumns)
                        {
                            var sName = '';
                            if ( bTrafficData )
                            {
                                var nArrIdx = arrColInf0.findIndex((x) => x.col === sKey);
                                if ( nArrIdx >= 0 )
                                {
                                    sName = ' : <span style="color:#F08080">' + arrColInf0[nArrIdx].name + '</span>';
                                }
                            }
                            else
                            {
                                var nArrIdx = arrColInf1.findIndex((x) => x.col === sKey);
                                if ( nArrIdx >= 0 )
                                {
                                    sName = ' : <span style="color:#F08080">' + arrColInf1[nArrIdx].name + '</span>';
                                }
                            }
                            var sItem = '';
                            if ( g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].bNumeric )
                            {
                                var nRowCnt = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount;
                                var fPercentage = (g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nZeros/nRowCnt)*100;
                                if ( nIdxF == 0 )
                                {
                                    sItem = "<tr style='height:20px;'><td colspan=3 class='font-weight-bold'>" + sKey + " (Numeric)" + sName + "</td></tr>";
                                    sItem += "<tr>"
                                        + "<td class='text-right' style='width:5rem'>count</td>"
                                        + "<td class='text-right'>" + nRowCnt + "</td>"
                                        + "<td rowspan=7><div style='padding-left:5px;height:300px;'>"
                                        + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas + "' style='width:360px;height:300px;'></canvas>"
                                        + "</div></td>"
                                        + "</tr>";
                                }
                                else
                                {
                                    sItem = "<tr style='height:20px;'><td colspan=3>&nbsp;</td></tr>";
                                    sItem += "<tr>"
                                        + "<td rowspan=7><div style='padding-left:5px;height:300px;'>"
                                        + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas + "' style='width:360px;height:300px;'></canvas>"
                                        + "</div></td>"
                                        + "<td class='text-right' style='width:5rem'>count</td>"
                                        + "<td class='text-right'>" + nRowCnt + "</td>"
                                        + "</tr>";
                                }
                                sItem += "<tr>"
                                    + "<td class='text-right' style='width:5rem'>mean</td>"
                                    + "<td class='text-right'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:5rem'>std dev</td>"
                                    + "<td class='text-right'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fStdDev*100)/100).toFixed(2) + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:5rem'>zeros</td>"
                                    + "<td class='text-right'>" + (Math.round(fPercentage*100)/100).toFixed(2) + "%</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:5rem'>min</td>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMin + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:5rem'>median</td>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMedian + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:5rem'>max</td>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMax + "</td>"
                                    + "</tr>";
                            }
                            else
                            {
                                var sMaxKey = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].sMaxUniqueKey;
                                if ( nIdxF == 0 )
                                {
                                    sItem = "<tr style='height:20px;'><td colspan=3 class='font-weight-bold'>" + sKey + " (Categorical)" + sName + "</td></tr>";
                                    sItem += "<tr>"
                                    + "<td class='text-right' style='width:7rem'>count</th>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount + "</td>"
                                    + "<td rowspan=5><div style='padding-left:5px;height:300px;'>"
                                    + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas + "' style='width:360px;height:300px;'></canvas>"
                                    + "</div></td>"
                                    + "</tr>";
                                }
                                else
                                {
                                    sItem = "<tr style='height:20px;'><td colspan=3>&nbsp;</td></tr>"
                                    sItem += "<tr>"
                                    + "<td rowspan=5><div style='padding-left:5px;height:300px;'>"
                                    + "<canvas id='" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas + "' style='width:360px;height:300px;'></canvas>"
                                    + "</div></td>"
                                    + "<td class='text-right' style='width:7rem'>count</th>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount + "</td>"
                                    + "</tr>";
                                }
                                sItem += "<tr>"
                                    + "<td class='text-right' style='width:7rem'>unique</th>"
                                    + "<td class='text-right'>" + Object.keys(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr).length + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:7rem'>top</th>"
                                    + "<td class='text-right'>" + sMaxKey + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:7rem'>freq top</th>"
                                    + "<td class='text-right'>" + g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sMaxKey] + "</td>"
                                    + "</tr><tr>"
                                    + "<td class='text-right' style='width:7rem'>avg str len</th>"
                                    + "<td class='text-right'>" + (Math.round(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                                    + "</tr>";
                            }
                            sHtml[nIdxF] += sItem;
                        }
                        sHtml[nIdxF] += "</table>";
                        $(sCtrlID).html(sHtml[nIdxF]);
                    }
                    // Make High Chart Table
                    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
                    {
                        sCID = '#idTblHighChart'+nIdxF.toString();
                        $(sCID+' thead').empty();
                        $(sCID+' tbody').empty();
                        var strThead = "<tr>";
                        var strTbody = "<tr>";
                        for (var sKey in g_clsDatasetFair.arrOvColumns)
                        {
                            strThead += "<th class='text-center fixed_th td-sm' style='background-color:"+arrThdColor[nIdxF]+";'>"+sKey+"</th>";
                            strTbody += "<td class='text-center'><div id='idDivHC"+nIdxF.toString()+"_"+sKey+"'></div></th>";
                        }
                        strThead += "</tr>";
                        strTbody += "</tr>";
                        $(sCID+' thead').append(strThead);
                        $(sCID+' tbody').append(strTbody);
                    }
                    // draw charts
                    fnCreateChart();

                    return resolve(true);
                }
            }
        })
    });
}
let acceessableCount = 1;
function fnPrepareProc()
{
    return new Promise(function(resolve, reject)
    { 
        let sTag = '';
        let sVal = '';

        $('#idSelCol4Classify').empty();
        $('#idSelCol4SubGroup').empty();

        for (var sKey in g_clsDatasetFair.arrOvColumns)
        {
            sTag = "<option value='" + sKey + "'>" + sKey + "</option>";
            $('#idSelCol4Classify').append(sTag);
            $('#idSelCol4SubGroup').append(sTag);
        }
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
            
            // for preset
            if ( bTrafficData )
            {
                var sKey = arrColHdr0[arrPreset0[0]];
                $('#idSelCol4SubGroup').val(sKey).trigger('change');
                var nIdx = arrColInf0.findIndex((x) => x.col === sKey);
                if ( nIdx >= 0 )
                {
                    $('#idTxtClassifyLblA').val(arrColInf0[nIdx].item[0].eng);
                    $('#idTxtClassifyLblB').val(arrColInf0[nIdx].item[1].eng);
                }
            }
            else
            {
                var sKey = arrColHdr1[arrPreset1[0]];
                $('#idSelCol4SubGroup').val(sKey).trigger('change');
                var nIdx = arrColInf1.findIndex((x) => x.col === sKey);
                if ( nIdx >= 0 )
                {
                    $('#idTxtClassifyLblA').val(arrColInf1[nIdx].item[0].eng);
                    $('#idTxtClassifyLblB').val(arrColInf1[nIdx].item[1].eng);
                }
            }
        }

        sVal = $("#idSelCol4Classify option:first").val();
        $('#idSelCol4Classify').val(sVal).trigger('change');
        
        $('#idDivParamMLT0').show();
        $('#idDivParamMLT1').show();

        // for test
        if ( bTrafficData )
        {
            $('#idSelCol4Classify').val(arrColHdr0[arrPreset0[0]]).trigger('change');
            $('#idSelCol4SubGroup').val(arrColHdr0[arrPreset0[1]]).trigger('change');
        }
        else
        {
            $('#idSelCol4Classify').val(arrColHdr1[arrPreset1[0]]).trigger('change');
            $('#idSelCol4SubGroup').val(arrColHdr1[arrPreset1[1]]).trigger('change');
        }
        $('#idSelMLModel').val(g_arrMLModel[1].key).trigger('change');

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

var ptrChart = null;
function fnCreateChart()
{
    var arrListConfig = [];
    var arrListSeries = [];

    var nFileCnt = g_clsDatasetFair.objRawData.length;
    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
    {
        var arrConfig = [];
        var arrSeries = [];
        for (var sKey in g_clsDatasetFair.arrOvColumns)
        {
            if ( g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ptrChart != null )
            {
                g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ptrChart.destroy();
            }

            var idCanvas = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].idCanvas;
            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ctxCanvas = document.getElementById(idCanvas).getContext('2d');
            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ctxCanvas.canvas.width = 360;
            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ctxCanvas.canvas.height = 300;
            
            var arrDataset = [];
            arrDataset.push( {
                data: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal,
                backgroundColor: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr
            } ); 
            var arrLgdLbl = [];
            for (let nIdx in g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl)
            {
                var sVal = g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl[nIdx];
                if ( bTrafficData )
                {
                    var nIdxM = arrColInf0.findIndex((x) => x.col === sKey);
                    if ( nIdxM >= 0 )
                    {
                        var sLabel = sVal.toString();
                        if ( arrColInf0[nIdxM].item.length >= 0 )
                        {
                            var nIdxS = arrColInf0[nIdxM].item.findIndex((x) => x.key === sVal.toString());
                            if ( nIdxS >= 0 )
                            {
                                sLabel += ': ' + arrColInf0[nIdxM].item[nIdxS].name;
                            }
                        }
                        arrLgdLbl.push(sLabel);
                    }
                }
                else
                {
                    var nIdxM = arrColInf1.findIndex((x) => x.col === sKey);
                    if ( nIdxM >= 0 )
                    {
                        var sLabel = sVal.toString();
                        if ( arrColInf1[nIdxM].item.length >= 0 )
                        {
                            var nIdxS = arrColInf1[nIdxM].item.findIndex((x) => x.key === sVal.toString());
                            if ( nIdxS >= 0 )
                            {
                                sLabel += ': ' + arrColInf1[nIdxM].item[nIdxS].name;
                            }
                        }
                        arrLgdLbl.push(sLabel);
                    }
                }
            }
            var config =
            {
                type: 'bar',
                data: {
                    labels: arrLgdLbl,//g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdLbl,
                    datasets: arrDataset
                },
                options:
                {
                    scales: { xAxes: [ { ticks: { autoSkip: false, } } ], yAxes: [ { ticks: { beginAtZero: true, } } ] },
                    responsive: false, 
                    maintainAspectRatio: false,
                }
            };
            arrConfig.push({key: sKey, config: config});

            g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ptrChart
             = new Chart(g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].ctxCanvas, config);
            
            $('#'+idCanvas).on('click', function(e)
            {
                var id = e.target.id.substring(10);//'idCvsChart'+##
                var idx = id.substring(0,1);
                var key = id.substring(1);
                var nIdx = parseInt(idx);
                var nKey = parseInt(key);
                if ( nKey < 0 || arrListConfig[nIdx][nKey] == undefined )
                {
                    alert("invalid key!");
                    return;
                }
                $("#idDlgOverview").dialog("open");
                var nWW = $('#idDivCanvas').width();
                var nLW = (arrListConfig[nIdx][nKey].config.data.labels.length * 15);
                var nW = Math.max(nWW,nLW);
                var nH = $('#idDivCanvas').height();
                $('#idDivCanvas').empty();
                var $cvs = $("<canvas id='idCanvas' style='width:"+nW+"px;height:"+nH+"px;'></canvas>");
                $('#idDivCanvas').append($cvs);

                var ctxCanvas = document.getElementById('idCanvas').getContext('2d')
                ctxCanvas.canvas.width  = nW;
                ctxCanvas.canvas.height = nH;
                if ( ptrChart != null ) ptrChart.destroy();
                ptrChart = new Chart(ctxCanvas, arrListConfig[nIdx][nKey].config);
            });

            // High Chart
            var series = [];
            for (var i in g_clsDatasetFair.arrOvColumns[sKey].arrOvData[0].arrLgdVal)
            {
                var item = {data: []};
                item.data.push( {name: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl[i],
                    y: g_clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal[i] } );
                series.push(item);
            }
            arrSeries.push({key: sKey, data: series});
            var nW = $('#idDivTable').width();
            var nWid = (nW / Object.keys(g_clsDatasetFair.arrOvColumns).length) * 0.75;
            sCID = '#idDivHC'+nIdxF.toString()+'_'+sKey;
            $(sCID).width(nWid);
            $(sCID).highcharts(
            {
                chart: { type: 'column', },
                credits: { enabled: false },
                title: { text: '' },
                xAxis:{ categories: [sKey] },
                yAxis:
                {
                    min: 0, title: { text: '' }, labels: { x: -12 },
                    stackLabels: { enabled: true, style: { fontWeight: 'bold', color: (Highcharts.defaultOptions.title.style && Highcharts.defaultOptions.title.style.color)||'gray' } }
                },
                legend: { enabled: false },
                tooltip:
                {
                    headerFormat: '<b>{point.x}</b><br/>',
                    pointFormat: '{point.name}: {point.y}<br/>Total: {point.stackTotal}'
                },
                plotOptions: { column: { stacking: 'undefined', dataLabels: { enabled: true } } },
                series: series
            });
            $(sCID).on('click', function(e)
            {
                var id = e.currentTarget.id.substring(7);//'idDivHC'+#_$
                var idx = id.substring(0,1);
                var nIdx = parseInt(idx);
                var sKey = id.substring(2);
                if ( sKey == "" )
                {
                    alert("invalid key!");
                    return;
                }
                var nArrIdx = arrListSeries[nIdx].findIndex(x => x.key === sKey);
                if ( nArrIdx < 0 )
                {
                    alert("invalid key!");
                    return;
                }
                $('#idDlgHighChart').dialog("open");
                $('#idDivHighChart').highcharts(
                {
                    chart: { type: 'column', },
                    credits: { enabled: false },
                    title: { text: '' },
                    xAxis:{ categories: ['Before','After'] },
                    yAxis:
                    {
                        min: 0, title: { text: '' }, labels: { x: -12 },
                        stackLabels: { enabled: true, style: { fontWeight: 'bold', color: (Highcharts.defaultOptions.title.style && Highcharts.defaultOptions.title.style.color)||'gray' } }
                    },
                    legend: { enabled: false },
                    tooltip:
                    {
                        headerFormat: '<b>{point.x}</b><br/>',
                        pointFormat: '{point.name}: {point.y}<br/>Total: {point.stackTotal}'
                    },
                    plotOptions: { column: { stacking: 'undefined', dataLabels: { enabled: true } } },
                    series: arrListSeries[nIdx][nArrIdx].data
                });
            });
        }
        arrListConfig.push(arrConfig);
        arrListSeries.push(arrSeries);
    }
}

const arrStackClr = [ 'rgba(170, 200, 255, 1)', 'rgba(255, 170, 200, 1)' ];
var arrChartRst = [];
function fnDisplayResult(result)
{
    var MLTask = g_arrMLTask[g_oMLTaskIdx.mltask];
    var Parity = g_arrMLTask[g_oMLTaskIdx.mltask].parity[g_oMLTaskIdx.parity];
    
    if ( g_bOriginData )
    {
        for (let nIdx in result.data.confmat[0])
        {
            let sHtml = '';
            var sKey = $('#idSelCol4SubGroup option:selected').text();
            var arrVal = [];
            $('#idUlSubGroupVals li').each(function()
            {
                var sVal = $(this).text();
                arrVal.push(sVal);
            });
            var arrColInfo = ( bTrafficData ) ? arrColInf0 : arrColInf1;
            var nIdxM = arrColInfo.findIndex((x) => x.col === sKey);
            if ( nIdxM >= 0 )
            {
                sHtml += '<p><i class="fas fa-angle-double-right"></i> ' + arrColInfo[nIdxM].name;
                if ( arrColInfo[nIdxM].item.length >= 0 )
                {
                    var nIdxS = arrColInfo[nIdxM].item.findIndex((x) => x.key === arrVal[nIdx]);
                    if ( nIdxS >= 0 )
                    {
                        sHtml += ' <span style="color:#F08080">' + arrColInfo[nIdxM].item[nIdxS].name;
                    }
                }
                sHtml += '</span>에 대하여</p>';
            }
            if ( MLTask.key == 'binary' )
            {
                if ( Parity.key == 'equaloppo' )
                {
                    sHtml += '<p>TPR: ' + result.data.confmat[0][nIdx]['TPR'] + '%</p>';
                }
                else if ( Parity.key == 'equalodds' )
                {
                    sHtml += '<p>TPR: ' + result.data.confmat[0][nIdx]['TPR'] + '%</p>';
                    sHtml += '<p>FPR: ' + result.data.confmat[0][nIdx]['FPR'] + '%</p>';
                }
                else if ( Parity.key == 'dmgparity' )
                {
                    sHtml += '<p>TP: ' + result.data.confmat[0][nIdx]['TP'] + '</p>';
                    sHtml += '<p>FP: ' + result.data.confmat[0][nIdx]['FP'] + '</p>';
                    sHtml += '<p>TN: ' + result.data.confmat[0][nIdx]['TN'] + '</p>';
                    sHtml += '<p>FN: ' + result.data.confmat[0][nIdx]['FN'] + '</p>';
                }
            }
            else if ( MLTask.key == 'regres' )
            {
                if ( Parity.key == 'dmgparity' )
                {
                }
                else if ( Parity.key == 'bndgrplos' )
                {
                }
            }
            $('#idDivConfMatA'+nIdx).html(sHtml);
        }

        $('#idDivGraphA0').html(result.data.htmlimg[0][0]);
        $('#idDivGraphA1').html(result.data.htmlimg[0][1]);
        if ( result.data.htmlimg[0].length > 2 )
        {
            $('#idDivGraphA2').html(result.data.htmlimg[0][2]);
        }

        // var url = "http://164.125.37.214:5555/api/fairness/tpr"
        // if ( !bTrafficData ) url += "2";
        // var data =
        // {
        //     csv: result.filename,//"210719_fairness_test_origin chicago crashes.csv",//
        //     tpra: result.data.confmat[0][0]['TPR'],//"0.50",//
        //     tprb: result.data.confmat[0][1]['TPR'],//"0.163",//
        // };
        // $.ajax(
        // {
        //     url: url,
        //     dataType: 'JSON',
        //     method: 'GET',
        //     data: data,
        //     success: function(res)
        //     {
        //         if ( res.train != '' )
        //         {
        //             fnLoadFile(res.train, false);
        //         }
        //         else
        //         {
        //             alert("로딩된 데이터가 없습니다.");
        
        //             $('.loading').prop("disabled",false).removeClass('disabled');
        //             $('.running').prop("disabled",false).removeClass('disabled');
        //             $('#idBtnRunTPR').html(g_arrBtnText[1]);
                    
        //             return
        //         }
        //     },
        //     error: function(jqXHR, textStatus, errorThrown)
        //     {
        //         //alert( jqXHR.status );
        //         alert( jqXHR.statusText );
        //         //alert( jqXHR.responseText );
        //         //alert( jqXHR.readyState );
        //     }
        // })

        // for test
        fnLoadFile((bTrafficData)?"traffic_after.csv":"health_after.csv", false);
    }
    else
    {
        for (let nIdx in result.data.confmat[1])
        {
            let sHtml = '';
            var sKey = $('#idSelCol4SubGroup option:selected').text();
            var arrVal = [];
            $('#idUlSubGroupVals li').each(function()
            {
                var sVal = $(this).text();
                arrVal.push(sVal);
            });
            var arrColInfo = ( bTrafficData ) ? arrColInf0 : arrColInf1;
            var nIdxM = arrColInfo.findIndex((x) => x.col === sKey);
            if ( nIdxM >= 0 )
            {
                sHtml += '<p><i class="fas fa-angle-double-right"></i> ' + arrColInfo[nIdxM].name;
                if ( arrColInfo[nIdxM].item.length >= 0 )
                {
                    var nIdxS = arrColInfo[nIdxM].item.findIndex((x) => x.key === arrVal[nIdx]);
                    if ( nIdxS >= 0 )
                    {
                        sHtml += ' <span style="color:#F08080">' + arrColInfo[nIdxM].item[nIdxS].name;
                    }
                }
                sHtml += '</span>에 대하여</p>';
            }
            if ( MLTask.key == 'binary' )
            {
                if ( Parity.key == 'equaloppo' )
                {
                    sHtml += '<p>TPR: ' + result.data.confmat[1][nIdx]['TPR'] + '%</p>';
                }
                else if ( Parity.key == 'equalodds' )
                {
                    sHtml += '<p>TPR: ' + result.data.confmat[1][nIdx]['TPR'] + '%</p>';
                    sHtml += '<p>FPR: ' + result.data.confmat[1][nIdx]['FPR'] + '%</p>';
                }
                else if ( Parity.key == 'dmgparity' )
                {
                    sHtml += '<p>TP: ' + result.data.confmat[1][nIdx]['TP'] + '</p>';
                    sHtml += '<p>FP: ' + result.data.confmat[1][nIdx]['FP'] + '</p>';
                    sHtml += '<p>TN: ' + result.data.confmat[1][nIdx]['TN'] + '</p>';
                    sHtml += '<p>FN: ' + result.data.confmat[1][nIdx]['FN'] + '</p>';
                }
            }
            else if ( MLTask.key == 'regres' )
            {
                if ( Parity.key == 'dmgparity' )
                {
                }
                else if ( Parity.key == 'bndgrplos' )
                {
                }
            }
            $('#idDivConfMatB'+nIdx).html(sHtml);
        }

        $('#idDivGraphB0').html(result.data.htmlimg[1][0]);
        $('#idDivGraphB1').html(result.data.htmlimg[1][1]);
        if ( result.data.htmlimg[1].length > 2 )
        {
            $('#idDivGraphB2').html(result.data.htmlimg[1][2]);
        }
        
        var fMin = 100, fMax = 0;
        var arrDatasets = [];
        var arrLgdLabel = [];
        var nFileCnt = g_clsDatasetFair.objRawData.length;
        for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
        {
            var fVal = 0.0;
            var sLabel = $('#idSelCol4SubGroup option:selected').text();
            var arrDataset = [];
            var arrLgdLbl = [];
            if ( MLTask.key == 'binary' )
            {
                if ( Parity.key == 'equaloppo' )
                {
                    var arrLgdDat = [];
                    var nIdx = 0;
                    $('#idUlSubGroupVals li').each(function()
                    {
                        fVal = parseFloat(result.data.confmat[nIdxF][nIdx++]['TPR']) * 100;
                        fMin = Math.min(fMin, fVal); fMax = Math.max(fMax, fVal);
                        arrLgdDat.push(fVal.toFixed(3).toString());
                        arrLgdLbl.push(sLabel+'='+$(this).text());
                    });
                    arrDataset.push( { label: 'TPR', data: arrLgdDat, backgroundColor: arrStackClr[0] } );
                }
                else if ( Parity.key == 'equalodds' )
                {
                    var arrLgdDat0 = [];
                    var arrLgdDat1 = [];
                    var nIdx = 0;
                    $('#idUlSubGroupVals li').each(function()
                    {
                        fVal = parseFloat(result.data.confmat[nIdxF][nIdx]['TPR']) * 100;
                        fMin = Math.min(fMin, fVal); fMax = Math.max(fMax, fVal);
                        arrLgdDat0.push(fVal.toFixed(3).toString());
                        fVal = parseFloat(result.data.confmat[nIdxF][nIdx++]['FPR']) * 100;
                        fMin = Math.min(fMin, fVal); fMax = Math.max(fMax, fVal);
                        arrLgdDat1.push(fVal.toFixed(3).toString());

                        arrLgdLbl.push(sLabel+'='+$(this).text());
                    });
                    arrDataset.push( { label: 'TPR', data: arrLgdDat0, backgroundColor: arrStackClr[0] } );
                    arrDataset.push( { label: 'FPR', data: arrLgdDat1, backgroundColor: arrStackClr[1] } );
                }
                else if ( Parity.key == 'dmgparity' )
                {
                    fMax = 100;
                    var arrLgdDat0 = [];
                    var arrLgdDat1 = [];
                    var nIdx = 0;
                    var fTP, fFP, fTN, fFN;
                    $('#idUlSubGroupVals li').each(function()
                    {
                        fTP = parseFloat(result.data.confmat[nIdxF][nIdx]['TP']);
                        fFP = parseFloat(result.data.confmat[nIdxF][nIdx]['FP']);
                        fTN = parseFloat(result.data.confmat[nIdxF][nIdx]['TN']);
                        fFN = parseFloat(result.data.confmat[nIdxF][nIdx++]['FN']);

                        fVal = (fTP+fFP) * 100 / (fTP+fFP+fTN+fFN);
                        fMin = Math.min(fMin, fVal);
                        arrLgdDat0.push(fVal.toFixed(3).toString());
                        fVal = 100 - fVal;
                        arrLgdDat1.push(fVal.toFixed(3).toString());

                        arrLgdLbl.push(sLabel+'='+$(this).text());
                    });
                    arrDataset.push( { label: 'predicted condition positive (tp+fp)', data: arrLgdDat0, backgroundColor: arrStackClr[0] } );
                    arrDataset.push( { label: 'predicted condition negative (tn+fn)', data: arrLgdDat1, backgroundColor: arrStackClr[1] } );
                }
            }
            else if ( MLTask.key == 'regres' )
            {
                if ( Parity.key == 'dmgparity' )
                {
                }
                else if ( Parity.key == 'bndgrplos' )
                {
                }
            }
            arrDatasets.push(arrDataset);
            arrLgdLabel.push(arrLgdLbl);
        }
        fMin = Math.max(  0, Math.floor((fMin-5)/10)*10);
        fMax = Math.min(100, Math.round((fMax+5)/10)*10);
        
        for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
        {
            if ( arrChartRst[nIdxF] != null || arrChartRst[nIdxF] != undefined )
            {
                arrChartRst[nIdxF].destroy();
            }
            var idCanvas = 'idCanvas'+nIdxF.toString();
            var ctxCanvas = document.getElementById(idCanvas).getContext('2d');
            ctxCanvas.canvas.width = 480;
            ctxCanvas.canvas.height = 320;
            
            var scales = {
                xAxes: [ { ticks: { autoSkip: false } } ],
                yAxes: [ { ticks: { min: fMin, max: fMax } } ]
            };
            var animation = 
            {
                duration: 0,
                onComplete: function()
                {
                    var ctx = this.chart.ctx;
                    ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, 'normal', Chart.defaults.global.defaultFontFamily);
                    ctx.fillStyle = this.chart.config.options.defaultFontColor;
                    ctx.textBaseline = 'middle';
                    ctx.textAlign = 'center';

                    const lndash = [5, 5];
                    const lineH = ctx.measureText('M').width;

                    this.data.datasets.forEach(function (dataset)
                    {
                        for (var i = 0; i < dataset.data.length; i++)
                        {
                            var model = dataset._meta[Object.keys(dataset._meta)[0]].data[i]._model;
                            ctx.fillText(dataset.data[i]+'%', model.x, model.y+(model.base-model.y)*0.5);
                        }
                    });
                    
                    ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, 'bold', Chart.defaults.global.defaultFontFamily);
                    ctx.fillStyle = (nIdxF==0)?'red':'blue';
                    ctx.strokeStyle = (nIdxF==0)?'red':'blue';
                    ctx.textAlign = 'left';
                    ctx.lineWidth = 1;

                    this.data.datasets.forEach(function (dataset)
                    {
                        for (var i = 1; i < dataset.data.length; i++)
                        {
                            var d0 = dataset.data[i-1];
                            var d1 = dataset.data[i  ];
                            var m0 = dataset._meta[Object.keys(dataset._meta)[0]].data[i-1]._model;
                            var m1 = dataset._meta[Object.keys(dataset._meta)[0]].data[i  ]._model;
                            
                            let percent = Math.abs(d0 - d1);
                            let barValue = `${percent.toFixed(3)}%`;

                            if ( m0.y < m1.y ) // win. coord.
                            {
                                var x = m1.x+Math.round(m1.width*0.25);
                                var y = m1.y-Math.round((m1.y-m0.y)*0.5);
                                ctx.fillText(barValue, x+lineH, Math.max(lineH*1.5, y));
                                fnCvsArrow(ctx, x, m0.y, x, m1.y);
                                ctx.setLineDash(lndash);
                                ctx.beginPath();
                                ctx.moveTo(m0.x, m0.y);
                                ctx.lineTo(x, m0.y);
                                ctx.stroke();
                                ctx.beginPath();
                                ctx.moveTo(m1.x, m1.y);
                                ctx.lineTo(x, m1.y);
                                ctx.stroke();
                            }
                            else
                            {
                                var x = m0.x-Math.round(m1.width*0.25);
                                var y = m0.y-Math.round((m0.y-m1.y)*0.5);
                                ctx.fillText(barValue, x+lineH, Math.max(lineH*1.5, y));
                                fnCvsArrow(ctx, x, m0.y, x, m1.y);
                                ctx.setLineDash(lndash);
                                ctx.beginPath();
                                ctx.moveTo(m1.x, m1.y);
                                ctx.lineTo(x, m1.y);
                                ctx.stroke();
                                ctx.beginPath();
                                ctx.moveTo(m0.x, m0.y);
                                ctx.lineTo(x, m0.y);
                                ctx.stroke();
                            }
                        }
                    });
                }
            }
            if ( MLTask.key == 'binary' )
            {
                if ( Parity.key == 'equaloppo' )
                {
                }
                else if ( Parity.key == 'equalodds' )
                {
                }
                else if ( Parity.key == 'dmgparity' )
                {
                    scales = { 
                        xAxes: [ { stacked: true, ticks: { autoSkip: false } } ],
                        yAxes: [ { stacked: true, ticks: { min: fMin, max: fMax } } ]
                    };
                    animation = 
                    {
                        duration: 0,
                        onComplete: function()
                        {
                            var ctx = this.chart.ctx;
                            ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, 'normal', Chart.defaults.global.defaultFontFamily);
                            ctx.fillStyle = this.chart.config.options.defaultFontColor;
                            ctx.textBaseline = 'middle';
                            ctx.textAlign = 'center';
        
                            const lndash = [5, 5];
                            const lineH = ctx.measureText('M').width;
        
                            this.data.datasets.forEach(function (dataset)
                            {
                                for (var i = 0; i < dataset.data.length; i++)
                                {
                                    var model = dataset._meta[Object.keys(dataset._meta)[0]].data[i]._model;
                                    ctx.fillText(dataset.data[i]+'%', model.x, model.y+(model.base-model.y)*0.5);
                                }
                            });
                            
                            ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, 'bold', Chart.defaults.global.defaultFontFamily);
                            ctx.fillStyle = (nIdxF==0)?'red':'blue';
                            ctx.strokeStyle = (nIdxF==0)?'red':'blue';
                            ctx.textAlign = 'left';
                            ctx.lineWidth = 1;
        
                            if ( this.data.datasets.length > 0 )
                            {
                                dataset = this.data.datasets[0];
                                for (var i = 1; i < dataset.data.length; i++)
                                {
                                    var d0 = dataset.data[i-1];
                                    var d1 = dataset.data[i  ];
                                    var m0 = dataset._meta[Object.keys(dataset._meta)[0]].data[i-1]._model;
                                    var m1 = dataset._meta[Object.keys(dataset._meta)[0]].data[i  ]._model;
                                    
                                    let percent = Math.abs(d0 - d1);
                                    let barValue = `${percent.toFixed(3)}%`;
        
                                    if ( m0.y < m1.y ) // win. coord.
                                    {
                                        var x = m1.x+Math.round(m1.width*0.25);
                                        var y = m1.y-Math.round((m1.y-m0.y)*0.5);
                                        ctx.fillText(barValue, x+lineH, Math.max(lineH*1.5, y));
                                        fnCvsArrow(ctx, x, m0.y, x, m1.y);
                                        ctx.setLineDash(lndash);
                                        ctx.beginPath();
                                        ctx.moveTo(m0.x, m0.y);
                                        ctx.lineTo(x, m0.y);
                                        ctx.stroke();
                                        ctx.beginPath();
                                        ctx.moveTo(m1.x, m1.y);
                                        ctx.lineTo(x, m1.y);
                                        ctx.stroke();
                                    }
                                    else
                                    {
                                        var x = m0.x-Math.round(m1.width*0.25);
                                        var y = m0.y-Math.round((m0.y-m1.y)*0.5);
                                        ctx.fillText(barValue, x+lineH, Math.max(lineH*1.5, y));
                                        fnCvsArrow(ctx, x, m0.y, x, m1.y);
                                        ctx.setLineDash(lndash);
                                        ctx.beginPath();
                                        ctx.moveTo(m1.x, m1.y);
                                        ctx.lineTo(x, m1.y);
                                        ctx.stroke();
                                        ctx.beginPath();
                                        ctx.moveTo(m0.x, m0.y);
                                        ctx.lineTo(x, m0.y);
                                        ctx.stroke();
                                    }
                                }
                            };
                        }
                    }
                }
            }
            else if ( MLTask.key == 'regres' )
            {
                if ( Parity.key == 'dmgparity' )
                {
                }
                else if ( Parity.key == 'bndgrplos' )
                {
                }
            }
            
            var config =
            {
                type: 'bar',
                data: {
                    enabled: true,
                    labels: arrLgdLabel[nIdxF],
                    datasets: arrDatasets[nIdxF]
                },
                options:
                {
                    scales:scales,
                    responsive: false, 
                    maintainAspectRatio: false,
                    legend: { display: true, position: 'bottom', labels: {boxWidth: 10} },
                    events: [],
                    animation: animation
                }
            };
            arrChartRst[nIdxF] = new Chart(ctxCanvas, config);
        }


        $.ajax(
        {
            url: 'getresult/',
            type: 'POST',
            data: JSON.stringify({'key': g_sAccessKey}),
            dataType: 'JSON',
            success: function(res)
            {
                var result = JSON.parse(res);
                if ( !result.success )
                {
                    alert( result.message );
                }
                else
                {
                    var nArrIdx = result.data.corrate.findIndex(x => x.key === (MLTask.key+Parity.key));
                    var sName = result.data.corrate[nArrIdx].name;
                    var fRate = parseFloat(result.data.corrate[nArrIdx].rate);
                    sTag = '<p style="font-size:32px;">보정률 ('+sName+') : <span style="color:#F00;">'+fRate.toFixed(3)+' %</span></p>';
                    $('#idDivPerform').html(sTag);
    
                    // let sHtml = '';
                    // for (let nIdx in result.data.corrate)
                    // {
                    //     var sName = result.data.corrate[nIdx].name;
                    //     var fRate = parseFloat(result.data.corrate[nIdx].rate);
                    //     sHtml += '<p style="font-size:15px;">보정률 ('+sName+') : '+fRate.toFixed(3)+' % </p>';
                    // }
                    // $('#idDivPerform').html(sHtml);
                }
            }
        })

        g_bProcDone = true;

        fnCheckFileList(true);

        $('.loading').prop("disabled",false).removeClass('disabled');
        $('#idBtnRunTPR').html(g_arrBtnText[2] + ' (FINISHED)');
    }
}

function fnCvsArrow(context, fx, fy, tx, ty)
{
    const dx = tx - fx;
    const dy = ty - fy;
    const headlen = 10;
    const angle = Math.atan2( dy, dx );
    const arowlen = Math.sqrt(dx*dx+dy*dy);
    const linelen = headlen*2;
    context.setLineDash([]);
    if ( arowlen <= linelen )
    {
        context.beginPath();
        context.moveTo( fx, fy - linelen);
        context.lineTo( fx, fy );
        context.stroke();
        context.beginPath();
        context.moveTo( fx - headlen * Math.cos( angle - Math.PI / 6 ), fy - headlen * Math.sin( angle - Math.PI / 6 ) );
        context.lineTo( fx, fy );
        context.lineTo( fx - headlen * Math.cos( angle + Math.PI / 6 ), fy - headlen * Math.sin( angle + Math.PI / 6 ) );
        context.stroke();
        context.beginPath();
        context.moveTo( tx - headlen * Math.cos( -angle - Math.PI / 6 ), ty - headlen * Math.sin( -angle - Math.PI / 6 ) );
        context.lineTo( tx, ty );
        context.lineTo( tx - headlen * Math.cos( -angle + Math.PI / 6 ), ty - headlen * Math.sin( -angle + Math.PI / 6 ) );
        context.stroke();
        context.beginPath();
        context.moveTo( tx, ty );
        context.lineTo( tx, ty + linelen );
        context.stroke();
    }
    else
    {
        context.beginPath();
        context.moveTo( fx - headlen * Math.cos( -angle - Math.PI / 6 ), fy - headlen * Math.sin( -angle - Math.PI / 6 ) );
        context.lineTo( fx, fy );
        context.lineTo( fx - headlen * Math.cos( -angle + Math.PI / 6 ), fy - headlen * Math.sin( -angle + Math.PI / 6 ) );
        context.stroke();
        context.beginPath();
        context.moveTo( fx, fy );
        context.lineTo( tx, ty );
        context.stroke();
        context.beginPath();
        context.moveTo( tx - headlen * Math.cos( angle - Math.PI / 6 ), ty - headlen * Math.sin( angle - Math.PI / 6 ) );
        context.lineTo( tx, ty );
        context.lineTo( tx - headlen * Math.cos( angle + Math.PI / 6 ), ty - headlen * Math.sin( angle + Math.PI / 6 ) );
        context.stroke();
    }
}