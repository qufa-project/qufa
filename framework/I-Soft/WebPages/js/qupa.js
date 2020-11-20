
var arrColVal = ['posted_speed_limit', 'weather_condition', 'lighting_condition', 'first_crash_type', 'roadway_surface_cond', 'damage'];
var sBtnText = '';

$(document).ready(function()
{
    Chart.defaults.global.legend.display = false;
    
    sBtnText = $('#idSpanXls').html();
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
    link.href = "js/facets.html";
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
    var fileupload = $("#fileupload")[0];
    var whendone = function(datasetsList)
    {
        fnMakeOverView(datasetsList);

        var dive = $("#fdelem")[0];
        if (datasetsList.length < 2)
        {
            dive.data = datasetsList[0].data;
        }
        else
        {
            var newFeatureForDive = 'csv-source';
            var columns = datasetsList.length > 0 ? datasetsList[0].data.columns : [];
            while (columns.indexOf(newFeatureForDive) > -1)
            {
                newFeatureForDive = newFeatureForDive + newFeatureForDive;
            }
            datasetsList.forEach(function(dataset)
            {
                dataset.data.forEach(function(datapoint)
                {
                    datapoint[newFeatureForDive] = dataset.name;
                });
            });
            var alldata = datasetsList.reduce(function(a, b)
            {
                return a.concat(b.data);
            }, []);
            dive.data = alldata;
            dive.colorBy = newFeatureForDive;
        }
        
        $('#idSpanXls').html(sBtnText).removeClass('fileupload-processing');
    }
    var fileworker = new Worker('js/worker.js');
    fileworker.onmessage = function(e)
    {
        var datasetsList=e.data;
        whendone(datasetsList);
    };
    fileworker.onerror = function(e)
    {
        console.error('ERROR: Line ', e.lineno, ' in ', e.filename, ': ', e.message);
    };
    var readFileAsync = function()
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
        $('#idSpanXls').html("<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>").addClass('fileupload-processing');
        var objParam = { files: fileupload.files, columns: arrColVal };
        fileworker.postMessage(objParam);
    }
    fileupload.addEventListener('change', readFileAsync);
}

function fnMakeOverView(datasetsList)
{
    var arrDatasets = [];
    for (var nIdxF = 0; nIdxF < datasetsList.length; nIdxF++)
    {
        var bFirstRow = true;
        var objDataset = { aColInfo: [], oDataset: {} };
        objDataset.oDataset = datasetsList[nIdxF];

        for (var nIdxR = 0; nIdxR < objDataset.oDataset.data.length; nIdxR++)
        {
            for (var nKey in objDataset.oDataset.data.columns)
            {
                var sKey = objDataset.oDataset.data.columns[nKey];
                var item = objDataset.oDataset.data[nIdxR][sKey];

                if ( bFirstRow )
                {
                    if ( item == undefined )
                    {
                        delete objDataset.oDataset.data.columns[nKey];
                        continue;
                    }

                    var oColInfo = {};
                    var nVal = +item;
                    oColInfo['isNumeric'] = !isNaN(nVal);
                    if ( !oColInfo['isNumeric'] )
                    {
                        nVal = item.length;
                    }
                    oColInfo['Count'] = 1;
                    oColInfo['Error'] = 0;
                    oColInfo['Sum'] = nVal;
                    oColInfo['Mean'] = nVal;
                    oColInfo['StdDev'] = 0;
                    oColInfo['Zeros'] = (nVal==0) ? 1 : 0;
                    oColInfo['Min'] = nVal;
                    oColInfo['Median'] = 0;
                    oColInfo['Max'] = nVal;
                    oColInfo['Unique'] = [];
                    oColInfo['Unique'][item.toString()] = 1;

                    objDataset.aColInfo[sKey] = oColInfo;
                }
                else
                {
                    var nVal = +item;
                    if ( objDataset.aColInfo[sKey]['isNumeric'] )
                    {
                        if ( isNaN(nVal) )
                        {
                            nVal = 0;
                            oColInfo['Error']++;
                        }
                        else
                        {
                            objDataset.aColInfo[sKey]['Count']++;
                            objDataset.aColInfo[sKey]['Sum'] += nVal;
                            if ( objDataset.aColInfo[sKey]['Min'] > nVal ) objDataset.aColInfo[sKey]['Min'] = nVal;
                            if ( objDataset.aColInfo[sKey]['Max'] < nVal ) objDataset.aColInfo[sKey]['Max'] = nVal;
                            if ( nVal == 0 ) objDataset.aColInfo[sKey]['Zeros']++;
                            if ( objDataset.aColInfo[sKey]['Unique'][item.toString()] == undefined )
                            {
                                objDataset.aColInfo[sKey]['Unique'][item.toString()] = 0
                            }
                            objDataset.aColInfo[sKey]['Unique'][item.toString()]++;
                        }
                    }
                    else
                    {
                        nVal = item.length;
                        objDataset.aColInfo[sKey]['Count']++;
                        objDataset.aColInfo[sKey]['Sum'] += nVal;
                        if ( objDataset.aColInfo[sKey]['Min'] > nVal ) objDataset.aColInfo[sKey]['Min'] = nVal;
                        if ( objDataset.aColInfo[sKey]['Max'] < nVal ) objDataset.aColInfo[sKey]['Max'] = nVal;
                        if ( nVal == 0 ) objDataset.aColInfo[sKey]['Zeros']++;
                        if ( objDataset.aColInfo[sKey]['Unique'][item.toString()] == undefined )
                        {
                            objDataset.aColInfo[sKey]['Unique'][item.toString()] = 0
                        }
                        objDataset.aColInfo[sKey]['Unique'][item.toString()]++;
                    }
                    objDataset.aColInfo[sKey]['Mean'] = 0;
                    objDataset.aColInfo[sKey]['Median'] = 0;
                    objDataset.aColInfo[sKey]['StdDev'] = 0;
                }
            }
            bFirstRow = false;
        }
        arrDatasets.push(objDataset);
        
        var strContents = '';
        for (var nRow = 0; nRow < objDataset.oDataset.data.length; nRow++)
        {
            strContents += zeroPadding((nRow+1), objDataset.oDataset.data.length.toString().length);
            for (var nKey in objDataset.oDataset.data.columns)
            {
                var sKey = objDataset.oDataset.data.columns[nKey];

                strContents += '&nbsp;&nbsp;';
                strContents += objDataset.oDataset.data[nRow][sKey];
            }
            strContents += '<br />';
        }
        $('#idDivTxt').html(strContents);
        
        var sTableItem = '';
        $('#idTblNumeric tbody').empty();
        $('#idTblCategor tbody').empty();
        while (arrCharts.length > 0) { arrCharts.pop(); }
        var nIdx = 0;
        for (var nKey in objDataset.oDataset.data.columns)
        {
            var sKey = objDataset.oDataset.data.columns[nKey];

            arrCharts.push( new objChart() );

            if ( objDataset.aColInfo[sKey]['isNumeric'] )
            {
                objDataset.aColInfo[sKey]['Mean'] = objDataset.aColInfo[sKey]['Sum'] / objDataset.aColInfo[sKey]['Count'];

                var SDprep = 0;
                for (var nRow = 0; nRow < objDataset.oDataset.data.length; nRow++)
                {
                    SDprep += Math.pow((parseFloat(objDataset.oDataset.data[nRow][sKey]) - objDataset.aColInfo[sKey]['Mean']), 2);
                }
                objDataset.aColInfo[sKey]['StdDev'] = Math.sqrt(SDprep/objDataset.aColInfo[sKey]['Count']);

                var arrNumber = [];
                for (var sVal in objDataset.aColInfo[sKey]['Unique'])
                {
                    arrNumber.push( parseInt(sVal, 10) );
                    arrCharts[nIdx].arrLgdLbl.push( parseInt(sVal, 10) );
                    arrCharts[nIdx].arrLgdVal.push( parseInt(objDataset.aColInfo[sKey]['Unique'][sVal], 10) );
                    arrCharts[nIdx].arrLgdClr.push( sBarColor );
                }
                objDataset.aColInfo[sKey]['Median'] = median(arrNumber);

                objDataset.aColInfo[sKey]['Zeros'] = (objDataset.aColInfo[sKey]['Zeros']/objDataset.aColInfo[sKey]['Count'])*100;

                sTableItem = "<tr style='height:20px;'>"
                    + "<td colspan=7 class='font-weight-bold'>" + sKey + "</td>"
                    + "<td rowspan=2><div style='padding-left:15px;'>"
                    + "<canvas id='idCvsChart"+ nIdx.toString() +"' style='width:480px;height:320px;'></canvas>"
                    + "</div></td>"
                    + "</tr>"
                $('#idTblNumeric').append(sTableItem);
                sTableItem = "<tr>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Count'] + "</td>"
                    + "<td class='text-center'>" + (Math.round(objDataset.aColInfo[sKey]['Mean']  *100)/100).toFixed(2) + "</td>"
                    + "<td class='text-center'>" + (Math.round(objDataset.aColInfo[sKey]['StdDev']*100)/100).toFixed(2) + "</td>"
                    + "<td class='text-center'>" + (Math.round(objDataset.aColInfo[sKey]['Zeros'] *100)/100).toFixed(2) + "%</td>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Min'] + "</td>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Median'] + "</td>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Max'] + "</td>"
                    + "</tr>";
                $('#idTblNumeric').append(sTableItem);
            }
            else
            {
                objDataset.aColInfo[sKey]['Mean'] = objDataset.aColInfo[sKey]['Sum'] / objDataset.aColInfo[sKey]['Count'];
                
                var nSize = 0;
                var nMax = 0;
                var sMaxKey = '';
                for (var sVal in objDataset.aColInfo[sKey]['Unique'])
                {
                    var nVal = parseInt(objDataset.aColInfo[sKey]['Unique'][sVal], 10);
                    if ( nMax < nVal ) { nMax = nVal; sMaxKey = sVal; }
                    arrCharts[nIdx].arrLgdLbl.push( sVal );
                    arrCharts[nIdx].arrLgdVal.push( nVal );
                    arrCharts[nIdx].arrLgdClr.push( sBarColor );
                    nSize++;
                }

                sTableItem = "<tr style='height:20px;'>"
                    + "<td colspan=5 class='font-weight-bold'>" + sKey + "</td>"
                    + "<td rowspan=2><div style='padding-left:15px;'>"
                    + "<canvas id='idCvsChart"+ nIdx.toString() +"' style='width:480px;height:320px;'></canvas>"
                    + "</div></td>"
                    + "</tr>";
                $('#idTblCategor').append(sTableItem);
                sTableItem = "<tr>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Count'] + "</td>"
                    + "<td class='text-center'>" + nSize + "</td>"
                    + "<td class='text-center'>" + sMaxKey + "</td>"
                    + "<td class='text-center'>" + objDataset.aColInfo[sKey]['Unique'][sMaxKey] + "</td>"
                    + "<td class='text-center'>" + (Math.round(objDataset.aColInfo[sKey]['Mean']*100)/100).toFixed(2) + "</td>"
                    + "</tr>";
                $('#idTblCategor').append(sTableItem);
            }
            nIdx++;
        }
        fnCreateChart();
    }
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

//const sBarColor = 'rgba(170, 200, 255, 0.7)';
const sBarColor = 'rgba(255, 170, 200, 0.7)';
function objChart()
{
    this.ptrChart = null;
    this.ctxCanvas = null;
    this.arrLgdLbl = [];
    this.arrLgdVal = [];
    this.arrLgdClr = [];
}
var arrCharts = [];

function fnCreateChart()
{
    for (var idx in arrCharts)
    {
        if ( arrCharts[idx].ptrChart != null ) arrCharts[idx].ptrChart.destroy();

        var idCanvas = 'idCvsChart' + idx.toString();
        arrCharts[idx].ctxCanvas = document.getElementById(idCanvas).getContext('2d');
        arrCharts[idx].ctxCanvas.canvas.width = 480;
        arrCharts[idx].ctxCanvas.canvas.height = 320;
        
        var config = {
            type: 'bar',
            data: {
                labels: arrCharts[idx].arrLgdLbl,
                datasets: [ { data: arrCharts[idx].arrLgdVal, backgroundColor: arrCharts[idx].arrLgdClr, } ]
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
        arrCharts[idx].ptrChart = new Chart(arrCharts[idx].ctxCanvas, config);
    }
}