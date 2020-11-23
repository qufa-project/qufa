
var strBtnText = '';
var arrColVal = ['posted_speed_limit', 'weather_condition', 'lighting_condition', 'first_crash_type', 'roadway_surface_cond', 'damage'];

$(document).ready(function()
{
    Chart.defaults.global.legend.display = false;
    
    strBtnText = $('#idBtnRun').html();
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
        
        $('.btn').prop("disabled",false).removeClass('disabled');
        $('#idBtnRun').html(strBtnText).removeClass('fileupload-processing');
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
    
    $('#idBtnRun').on('click', function()
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
    
            $('.btn').prop("disabled",true).addClass('disabled');
            $('#idBtnRun').html("<span><i class='fas fa-spinner fa-spin'></i> Processing...</span>").addClass('fileupload-processing');
            readFileAsync();
        }
        else
        {
            alert("Train 파일이 지정되지 않았습니다.");
            return;
        }
    });
}

const arrBarColor = [ 'rgba(170, 200, 255, 0.9)', 'rgba(255, 170, 200, 0.9)' ];
class COverviewData
{
    constructor()
    {
        this.bNumeric = false;
        this.nCount = 0;
        this.nError = 0;
        this.fSum = 0;
        this.fMean = 0;
        this.fStdDev = 0;
        this.nZeros = 0;
        this.fMin = 0;
        this.fMedian = 0;
        this.fMax = 0
        this.sMaxUniqueKey = ''; // key of max count
        this.pnUniqueCntr = []; // unique as key
        // for chart ctrl
        this.arrLgdLbl = [];
        this.arrLgdVal = [];
        this.arrLgdClr = [];
    }
}
class COvColumn
{
    constructor()
    {
        this.arrOvData = []; // ov data for train & test
        // for chart ctrl
        this.idCanvas = '';
        this.ctxCanvas = null;
        this.ptrChart = null;
    }
}
class CDatasetFair
{
    constructor()
    {
        this.objRawData = []; // raw data for train & test
        this.arrOvColumns = []; // overveiw columns
    }
}
var clsDatasetFair = null;

function fnMakeOverView(datasetsList)
{
    $('#idTblNumeric tbody').empty();
    $('#idTblCategor tbody').empty();

    if ( clsDatasetFair != null ) clsDatasetFair = null;
    clsDatasetFair = new CDatasetFair();
    
    var sTableItem = '';
    var nFileCnt = (datasetsList.length > 2) ? 2 : datasetsList.length;
    for (var nIdxF = 0; nIdxF < nFileCnt; nIdxF++)
    {
        clsDatasetFair.objRawData.push( datasetsList[nIdxF] );
        
        const nStrLen = clsDatasetFair.objRawData[nIdxF].data.length.toString().length;
        
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
        for (var nRow in clsDatasetFair.objRawData[nIdxF].data)
        {
            var nNum = +nRow;
            if ( isNaN(nNum) ) continue;
            
            strContents += "<tr><td class='text-right td-sm'>" + (nNum+1).toString() + "</td>";
            for (var nCol in clsDatasetFair.objRawData[nIdxF].data.columns)
            {
                var sKey = clsDatasetFair.objRawData[nIdxF].data.columns[nCol];

                strContents += "<td class='text-center td-sm'>";
                strContents += clsDatasetFair.objRawData[nIdxF].data[nRow][sKey];
                strContents += '</td>';
            }
            strContents += '</tr>';
        }
        $('#idTblRawData'+nIdxF.toString()+' tbody').append(strContents);

        // Make Overview
        for (var nCol in clsDatasetFair.objRawData[nIdxF].data.columns)
        {
            var sKey = clsDatasetFair.objRawData[nIdxF].data.columns[nCol];

            if ( clsDatasetFair.arrOvColumns[sKey] == undefined )
            {
                clsDatasetFair.arrOvColumns[sKey] = new COvColumn();
            }
            if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF] == undefined )
            {
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF] = new COverviewData();
            }

            for (var nRow in clsDatasetFair.objRawData[nIdxF].data)
            {
                if ( isNaN(+nRow) ) continue;
                var item = clsDatasetFair.objRawData[nIdxF].data[nRow][sKey];

                if ( nRow == 0 )
                {
                    if ( item == undefined )
                    {
                        clsDatasetFair.objRawData[nIdxF].data.columns[nCol] = undefined;
                        break;
                    }
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].bNumeric = !isNaN( +item );
                }
                
                var fVal = +item;
                if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].bNumeric )
                {
                    if ( isNaN(fVal) )
                    {
                        clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nError++;
                        continue;
                    }
                }
                else
                {
                    fVal = item.length;
                    if ( fVal == 0 )
                    {
                        clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nError++;
                        continue;
                    }
                }
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount++;
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fSum += fVal;
                var fPrevMin = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMin;
                var fPrevMax = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMax;
                if ( fPrevMin > fVal ) clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMin = fVal;
                if ( fPrevMax < fVal ) clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMax = fVal;
                if ( fVal == 0 ) clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nZeros++;
                if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[item.toString()] == undefined )
                {
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[item.toString()] = 0
                }
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[item.toString()]++;
            }

            var nRowCnt = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount;
            clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean = clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fSum / nRowCnt;

            // Make Chart Data
            clsDatasetFair.arrOvColumns[sKey].idCanvas = 'idCvsChart' + nCol.toString();
            if ( clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].bNumeric )
            {
                var SDprep = 0;
                for (var nRow in clsDatasetFair.objRawData[nIdxF].data)
                {
                    if ( isNaN(+nRow) ) continue;
                    var fVal = +clsDatasetFair.objRawData[nIdxF].data[nRow][sKey];
                    SDprep += Math.pow((fVal - clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean), 2);
                }
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fStdDev = Math.sqrt(SDprep / nRowCnt);

                var nUniqueCnt = 0;
                var arrNumber = [];
                for (var sVal in clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr)
                {
                    var fVal = +sVal;
                    arrNumber.push( fVal );
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl.push( fVal );
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal.push( +clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sVal] );
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr.push( arrBarColor[nIdxF] );
                    nUniqueCnt++;
                }
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr.length = nUniqueCnt;
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMedian = median(arrNumber);
            }
            else
            {
                var nUniqueCnt = 0;
                var nMax = 0;
                var sMaxKey = '';
                for (var sVal in clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr)
                {
                    var nLen = +clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sVal];
                    if ( nMax < nLen ) { nMax = nLen; sMaxKey = sVal; }
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdLbl.push( sVal );
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdVal.push( nLen );
                    clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].arrLgdClr.push( arrBarColor[nIdxF] );
                    nUniqueCnt++;
                }
                clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr.length = nUniqueCnt;
                clsDatasetFair.arrOvColumns[sKey].sMaxUniqueKey = sMaxKey;
            }
        }
    }
    // Make Table
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
                var sMaxKey = clsDatasetFair.arrOvColumns[sKey].sMaxUniqueKey;
                sTableItem = "<tr>"
                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].nCount + "</td>"
                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr.length + "</td>"
                    + "<td class='text-center'>" + sMaxKey + "</td>"
                    + "<td class='text-center'>" + clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].pnUniqueCntr[sMaxKey] + "</td>"
                    + "<td class='text-center'>" + (Math.round(clsDatasetFair.arrOvColumns[sKey].arrOvData[nIdxF].fMean*100)/100).toFixed(2) + "</td>"
                    + "</tr>";
                $('#idTblCategor').append(sTableItem);
            }
        }
    }
    fnCreateChart();
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