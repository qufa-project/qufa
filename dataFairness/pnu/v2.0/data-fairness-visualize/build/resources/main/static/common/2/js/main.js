$(document).ready(function () {
    init();
});

Number.prototype.format = function () {
    if (this == 0) return 0;
    var reg = /(^[+-]?\d+)(\d{3})/;
    var n = (this + '');
    while (reg.test(n)) n = n.replace(reg, '$1' + ',' + '$2');
    return n;
};

String.prototype.format = function () {
    var num = parseFloat(this);
    if (isNaN(num)) return '0';
    return num.format();
};

var DATA_STEP1, DATA_STEP2, DATA_STEP3, DATA_STEP4, DATA_STEP5, DATA_REPORT;
var StepIdx = 0;
var Step2MaxColumn = 5;

function init() {
}

function startProgress(text) {
    // $('#progress').css('width', '100%');
    if (!text) {
        text = 'Running...';
    }
    $('body').loadingModal({
        text: text
    });
}

function endProgress() {
    setTimeout(function () {
        $('#progress').css('width', '0');
        $('body').loadingModal('destroy');
    }, 500);
}

function openModal(idx) {
    $('#modal-' + idx + '-button').trigger('click');
}

function closeModal(idx) {
    $('#modal-' + idx + '-close').trigger('click');
}

function rewindStep() {
    var id = '#step' + StepIdx + '-box';
    $(id).children().fadeOut("fast", function () {
        $(id).empty();
    });
    var domA = document.getElementById('section' + String(StepIdx));
    if (domA) {
        domA.classList.remove('section-active');
        domA.classList.remove('section-complete');
    }
    var domB = document.getElementById('section' + String(StepIdx - 1));
    if (domB) {
        domB.classList.remove('section-complete');
        domB.classList.add('section-active');
    }
    var domC = document.getElementById('btn-next' + String(StepIdx - 1));
    if (domC) {
        domC.disabled = false;
    }
    var domD = document.getElementById('btn-back' + String(StepIdx - 1));
    if (domD) {
        domD.disabled = false;
    }
    StepIdx--;
    moveScroll(StepIdx);
}

function prepareStep() {
    var domB = document.getElementById('section' + String(StepIdx));
    if (domB) {
        domB.classList.remove('section-active');
        domB.classList.add('section-complete');
        var domC = document.getElementById('btn-next' + String(StepIdx));
        if (domC) {
            domC.disabled = true;
        }
        var domD = document.getElementById('btn-back' + String(StepIdx));
        if (domD) {
            domD.disabled = true;
        }
    }
    StepIdx++;
    moveScroll(StepIdx);
    var domA = document.getElementById('section' + String(StepIdx));
    domA.classList.add('section-active');
}

function moveScroll(i) {
    // var dom = document.querySelector('#section' + i);
    // if (dom) {
    //     var offset = dom.offsetTop;
    //     window.scrollTo({top: offset, behavior: 'smooth'});
    // }
    var offset = $('#section' + i).offset();
    $('html, body').animate({scrollTop: offset.top}, 'slow');
}

function onReset() {
    StepIdx = 0, DATA_STEP1 = DATA_STEP2 = DATA_STEP3 = DATA_STEP4 = DATA_STEP5 = DATA_REPORT = undefined;
    document.getElementById('btn-start').disabled = false;
    document.getElementById('form-start').reset();
    for (var i = 1; i <= Step2MaxColumn; i++) {
        document.getElementById('step' + String(i) + '-box').innerHTML = '';
        document.getElementById('section' + String(i)).classList.remove('section-complete');
        document.getElementById('section' + String(i)).classList.remove('section-active');
    }
    closeModal(1);
}

function onStart() {
    document.getElementById('btn-start').disabled = true;
    var form = document.getElementById('form-start');
    var data = new FormData(form);
    $.ajax({
        type: 'post',
        enctype: 'multipart/form-data',
        url: '/api/module2/submitUpload',
        data: data,
        processData: false,
        contentType: false,
        cache: false,
        success: function (result) {
            onStartCallback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        beforeSend: function () {
            startProgress('Running Upload & Data Initialize Module...');
        },
        xhr: function () {
            var xhr = new window.XMLHttpRequest();
            $('#progress').css('width', '0');
            xhr.upload.addEventListener('progress', function (evt) {
                if (evt.lengthComputable) {
                    var percentComplete = evt.loaded / evt.total;
                    percentComplete = parseInt(percentComplete * 100);
                    // console.log(percentComplete);
                    $('#progress').css('width', percentComplete + '%');
                    if (percentComplete === 100) {
                    }
                }
            }, false);
            return xhr;
        },
    });
    return false;
}

function onStartCallback(result) {
    DATA_STEP1 = result;
    console.log('DATA_STEP1', DATA_STEP1);
    initStep1();
}

function initStep1() {
    prepareStep();
    $('#step' + StepIdx + '-box').append(
        $('<hr>'),
        $('<table>').attr('id', 'step' + StepIdx + '-table').attr('class', 'table table-dark').append(
            $('<tr>').append(
                $('<td>').append(
                    $('<div>').attr('class', 'row').append(
                        // $('<div>').attr('class', 'col-2').append(
                        //     'Binary Classification'
                        // ),
                        $('<div>').attr('class', 'col').attr('id', 'step' + StepIdx + '-radios').append(
                        )
                    )
                )
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-5 w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                        .attr('onclick', 'onNextButtonStep' + StepIdx + '()').append('Next')
                )
            )
        )
    );
    var header = DATA_STEP1.data.header;
    var binary = DATA_STEP1.data.binary;
    for (var i = 0; i < header.length; i++) {
        $('#step' + StepIdx + '-radios').append(
            $('<div>').attr('class', 'form-check form-check-inline').append(
                $('<input>').attr('class', 'form-check-input').attr('type', 'radio')
                    .attr('id', 'columnOrder0id' + String(i)).attr('name', 'columnOrder0').attr('value', String(i))
                    .attr('disabled', !binary.includes(header[i])),
                // .prop('checked', i < 1),
                $('<label>').attr('class', 'form-check-label').attr('for', 'columnOrder0id' + String(i)).append(header[i].toUpperCase())
            )
        );
    }
    endProgress();
}

function onNextButtonStep1() {
    var checkedRadio = document.querySelector('input[name="columnOrder0"]:checked');
    if (checkedRadio) {
        document.getElementById('step1-table').style.border = '';
    } else {
        document.getElementById('step1-table').style.border = '3px dotted red';
        return;
    }
    var columnOrder0val = checkedRadio.value;
    var header = DATA_STEP1.data.header;
    for (var i = 0; i < header.length; i++) {
        var dom = document.getElementById('columnOrder0id' + String(i));
        if (dom && columnOrder0val != i) {
            dom.disabled = true;
        }
    }
    var data = {
        tablename: DATA_STEP1.data.tablename,
        header: DATA_STEP1.data.header,
        order0: header[columnOrder0val]
    };
    $.ajax({
        type: 'post',
        url: '/fairness/module2/step2',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            onNextButtonStep1Callback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        beforeSend: function () {
            startProgress('Running Data Analysis Module...');
        },
        complete: function () {
            // endProgress();
        },
    });
}

function onNextButtonStep1Callback(result) {
    DATA_STEP2 = result;
    console.log('DATA_STEP2', DATA_STEP2);
    initStep2();
}

function initStep2() {
    prepareStep();
    var countColumn;
    if (DATA_STEP2.order_column) {
        countColumn = Math.min(Step2MaxColumn, DATA_STEP2.order_column.length);
    }
    var range1DefaultValue = '1';
    var range2DefaultValue = String(Math.min(3, countColumn - 1));
    $('#step' + StepIdx + '-box').append(
        $('<hr>'),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'form-group').append(
                    $('<div>').attr('class', 'row').append(
                        $('<div>').attr('class', 'col').append(
                            $('<span>').append('Permutation : ').append(
                                $('<span>').attr('id', 'permutation_count_value').append(range2DefaultValue),
                                $('<span>').append(' of ' + (DATA_STEP2.order_column.length - 1)),
                            )
                        ),
                        $('<div>').attr('class', 'col text-right').append(
                            $('<span>').attr('class', 'pr-1 text-right').attr('id', 'permutation_warning').append()
                        )
                    ),
                    // $('<label>').attr('for', 'permutation_count').append('Permutation : ').append(
                    //     $('<span>').attr('id', 'permutation_count_value').append(range2DefaultValue),
                    //     $('<span>').append(' of ' + (DATA_STEP2.order_column.length - 1)),
                    //     $('<span>').attr('class', 'text-right').attr('id', 'permutation_warning').append()
                    // ),
                    $('<input>').attr('type', 'range').attr('class', 'form-control-range').attr('id', 'permutation_count')
                        .attr('min', '1').attr('max', DATA_STEP2.order_column.length - 1).attr('step', '1').attr('name', 'permutation_count').attr('value', range2DefaultValue)
                        .attr('onchange', 'step2OnChangePermutation(this.value)')
                )
            ),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'form-group').append(
                    $('<div>').attr('class', 'row').append(
                        $('<div>').attr('class', 'col').append(
                            $('<span>').append('Average Cut Repeat : ').append(
                                $('<span>').attr('id', 'avg_loop_limit_value').append(range1DefaultValue)
                            )
                        )
                    ),
                    // $('<label>').attr('for', 'avg_loop_limit').append('Average Recursion : ').append(
                    //     $('<span>').attr('id', 'avg_loop_limit_value').append(range1DefaultValue)
                    // ),
                    $('<input>').attr('type', 'range').attr('class', 'form-control-range').attr('id', 'avg_loop_limit')
                        .attr('min', '0').attr('max', '5').attr('step', '1').attr('name', 'avg_loop_limit').attr('value', range1DefaultValue)
                    // .attr('onchange', 'step2OnChangeAvgLoopRange(this.value)')
                )
            )
        ),
        // $('<div>').attr('id', 'step2CheckTitle').attr('class', 'todo').append('{Title.Chart.Case1}'),
        $('<div>').attr('class', 'row row-cols-' + countColumn + ' pt-3').attr('id', 'step' + StepIdx + '-box-row'),
    );
    var range1 = document.getElementById('avg_loop_limit');
    var range2 = document.getElementById('permutation_count');
    range1.addEventListener('input', updateRange1Value);
    range2.addEventListener('input', updateRange2Value);
    step2OnChangePermutation(range2DefaultValue);

    for (var i = 0; i < countColumn; i++) {
        var sd = Number(DATA_STEP2.stddev[DATA_STEP2.order_column[i]]);
        sd = sd.toFixed(4);
        $('#step' + StepIdx + '-box-row').append(
            $('<div>').attr('class', 'col').append(
                //* Step2.Checkbox.Case2
                // $('<div>').attr('class', 'text-center').append(
                //     $('<label>').attr('class', 'switch').append(
                //         $('<input>').attr('type', 'checkbox').attr('checked', i < 1)
                //             .attr('id', 'step2Checkbox' + i).attr('name', 'step2Checkbox')
                //             .attr('onclick', 'step2CheckboxControl(' + i + ')')
                //             .attr('checked', i < 1)
                //             .attr('disabled', i > 1),
                //         $('<span>').attr('class', 'slider')
                //     )
                // ),
                // */
                //* Step2.Checkbox.Case1
                // $('<div>').attr('class', 'form-check form-check-inline').append(
                //     $('<input>').attr('class', 'form-check-input').attr('type', 'checkbox')
                //         .attr('id', 'step2Checkbox' + i).attr('name', '')
                //         .attr('onclick', 'step2CheckboxControl(' + i + ')')
                //         .attr('checked', i < 1)
                //         .attr('disabled', i > 1),
                //     $('<label>').attr('class', 'form-check-label h5').attr('for', 'step2Checkbox' + i)
                //         .append(DATA_STEP2.order_column[i].toUpperCase())
                // ),
                // */
                $('<div>').attr('id', 'step2ChartCase1' + i),
                $('<div>').attr('class', 'text-right').append(
                    $('<small>').append(
                        $('<em>').append('SD: ' + sd)
                    )
                )
            )
        );
    }
    $('#step' + StepIdx + '-box').append(
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    // $('<span>').attr('class', '').append('Total ' + DATA_STEP2.rows.format() + ' rows.')
                )
            )
        ),
        // $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case2}'),
        // $('<div>').attr('class', 'row').append(
        //     $('<div>').attr('class', 'col').append(
        //         $('<div>').attr('id', 'step2ChartCase2')
        //     )
        // ),
        // $('<div>').attr('class', 'row').append(
        //     $('<div>').attr('class', 'col').append(
        //         $('<div>').attr('class', 'mt-3').append(
        //             $('<span>').attr('class', '').append('Total ' + DATA_STEP2.rows.format() + ' rows.')
        //         )
        //     )
        // ),
        // $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case3}'),
        $('<div>').attr('class', 'row mt-3').append(
            $('<div>').attr('class', 'col').attr('style', 'height: 750px').append(
                $('<div>').attr('id', 'step2ChartCase3')
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    $('<span>').attr('class', '').append('Total ' + DATA_STEP2.rows.format() + ' rows.')
                )
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-back' + StepIdx).attr('class', 'btn btn-dark mr-3')
                        .attr('onclick', 'onBackButtonStep' + StepIdx + '()').append('Back'),
                    $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                        .attr('onclick', 'onNextButtonStep' + StepIdx + '()').append('Next')
                )
            )
        )
    );
    initStep2Callback();
}

function initStep2Callback() {
    for (var i = 0; i < Step2MaxColumn; i++) {
        drawStep2ChartCase1(DATA_STEP2, 'step2ChartCase1', i);
    }
    endProgress();
    getStep2ChartCase2Data();
}

function step2CheckboxControl(i) {
    var checkbox = document.getElementById('step2Checkbox' + i);
    for (var j = i + 1; j < Step2MaxColumn; j++) {
        var checkbox1 = document.getElementById('step2Checkbox' + j);
        if (checkbox1) {
            checkbox1.checked = false;
            checkbox1.disabled = true;
        }
    }
    var checkbox1 = document.getElementById('step2Checkbox' + (i + 1));
    if (checkbox1) {
        checkbox1.disabled = !checkbox.checked;
    }
}

/*
function step2OnChangeAvgLoopRange(i) {
    var dom = document.getElementById('avg_loop_limit_value')
    if (dom) {
        dom.innerHTML = i;
    }
}

function step2OnChangePermutation(i) {
    var dom = document.getElementById('permutation_count_value')
    if (dom) {
        dom.innerHTML = i;
    }
}
 */

function updateRange1Value(e) {
    var dom = document.getElementById('avg_loop_limit_value')
    if (dom) {
        dom.innerHTML = e.target.value;
    }
}

function updateRange2Value(e) {
    var dom = document.getElementById('permutation_count_value')
    if (dom) {
        dom.innerHTML = e.target.value;
    }
}

function step2OnChangePermutation(r) {
    var n = DATA_STEP2.order_column.length - 1;
    var c = 1;
    for (var i = n; i > (n - Number(r)); i--) {
        c = c * i;
    }
    var dom = document.getElementById('permutation_warning');
    if (c > 300) {
        dom.innerHTML = c.format() + ' Cases (TOO MANY)';
        dom.classList.add('text-danger', 'font-weight-bold');
    } else {
        dom.innerHTML = c.format() + ' Cases';
        dom.classList.remove('text-danger', 'font-weight-bold');
    }
}

// Step2.Chart.Case1: highcharts Stacked column (https://www.highcharts.com/demo/column-stacked)
function drawStep2ChartCase1(dataSet, chartId, idx) {
    var title = dataSet.data_meta[0]['order' + idx];
    if (!title) {
        return;
    }
    var categories = step2Chart1GetCategories(dataSet, idx);
    var series = step2Chart1GetSeries(dataSet, idx);
    Highcharts.chart(chartId + idx, {
        chart: {
            type: 'column',
        },
        credits: {
            enabled: false
        },
        title: {
            text: title
        },
        xAxis: {
            categories: categories
        },
        yAxis: {
            min: 0,
            title: {
                // text: 'Total Data Consumption'
                text: ''
            },
            labels: {
                x: -12
            },
            stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: ( // theme
                        Highcharts.defaultOptions.title.style &&
                        Highcharts.defaultOptions.title.style.color
                    ) || 'gray'
                }
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            headerFormat: '<b>{point.x}</b><br/>',
            pointFormat: '{series.name}: {point.y}<br/>Total: {point.stackTotal}'
        },
        plotOptions: {
            column: {
                stacking: 'undefined', // undefined, normal, percent
                dataLabels: {
                    enabled: true
                }
            }
        },
        series: series
    });
}

function step2Chart1GetCategories(dataSet, idx) {
    var meta = dataSet.data_meta[0];
    var categories = [];
    if (meta['order' + idx]) {
        categories[0] = meta['order' + idx];
    }
    return categories;
}

function step2Chart1GetSeries(dataSet, idx) {
    var meta = dataSet.data_meta[0];
    var columnName = meta['order' + idx];
    var columnData = dataSet.column_data[columnName];
    var series = [];
    for (var i = 0; i < columnData.length; i++) {
        var col = columnData[i]['column_value'];
        var cnt = columnData[i]['count'];
        var a = {};
        a.data = [];
        a.data.push(cnt);
        a.name = col;
        series.push(a);
    }
    return series;
}

function getStep2ChartCase2Data() {
    var data = {
        tablename: DATA_STEP2.tablename,
        columnlist: DATA_STEP2.order_column
    };
    $.ajax({
        type: 'post',
        url: '/fairness/module2/step2Chart2Data',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            getStep2ChartCase2DataCallback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        // beforeSend: function () {
        //     startProgress();
        // },
        complete: function () {
            // endProgress();
        },
    });
}

function getStep2ChartCase2DataCallback(result) {
    // console.log('CHART_STEP2', result);
    // drawStep2ChartCase2(result);
    drawStep2ChartCase3(result);
}

// Step2.Chart.Case2: d3 Icicle (https://observablehq.com/@d3/icicle, https://github.com/vasturiano/icicle-chart)
function drawStep2ChartCase2(data) {
    const width = window.innerWidth - 88;
    const color = d3.scaleOrdinal(d3.schemePaired);
    Icicle()
        .orientation('lr')
        .data(data)
        .size('size')
        .width(width)
        .color((d, parent) => color(parent ? parent.data.name : null))
        .excludeRoot(true)
        (document.getElementById('step2ChartCase2'));
}

// Step2.Chart.Case3: d3 Sunburst (https://observablehq.com/@kerryrodden/sequences-sunburst, https://github.com/vasturiano/sunburst-chart)
function drawStep2ChartCase3(data) {
    const color = d3.scaleOrdinal(d3.schemePaired);
    Sunburst()
        .data(data)
        .size('size')
        .height(732)
        .color((d, parent) => color(parent ? parent.data.name : null))
        .excludeRoot(true)
        .radiusScaleExponent(1)
        (document.getElementById('step2ChartCase3'));
}

function onBackButtonStep2() {
    rewindStep();
    DATA_STEP2 = undefined;
    var header = DATA_STEP1.data.header;
    var binary = DATA_STEP1.data.binary;
    for (var i = 0; i < header.length; i++) {
        var dom = document.getElementById('columnOrder0id' + String(i));
        if (dom && binary.includes(header[i])) {
            dom.disabled = false;
        }
    }
}

function onNextButtonStep2() {
    var countColumn;
    if (DATA_STEP2.order_column) {
        countColumn = DATA_STEP2.order_column.length < Step2MaxColumn ? DATA_STEP2.order_column.length : Step2MaxColumn;
    }
    // var checkeded = document.querySelector('input[name="step2Checkbox"]:checked');
    // if (checkeded) {
    //     document.getElementById('step2CheckTitle').style.border = '';
    // } else {
    //     document.getElementById('step2CheckTitle').style.border = '3px dotted red';
    //     return;
    // }
    var range1 = document.getElementById('avg_loop_limit');
    if (range1) {
        range1.disabled = true;
    }
    var range2 = document.getElementById('permutation_count');
    if (range2) {
        range2.disabled = true;
    }
    var domC = document.getElementById('btn-next' + String(StepIdx));
    if (domC) {
        domC.disabled = true;
    }
    var domD = document.getElementById('btn-back' + String(StepIdx));
    if (domD) {
        domD.disabled = true;
    }
    // for (var i = 0; i < countColumn; i++) {
    //     var checkbox = document.getElementById('step2Checkbox' + i);
    //     if (checkbox) {
    //         checkbox.disabled = true;
    //     }
    // }
    // var order_column_new = DATA_STEP2.order_column;
    // for (var i = DATA_STEP2.order_column.length; i > countColumn; i--) {
    //     order_column_new.pop();
    // }
    /* USE Step2.Graph.Checkbox
     */
    // var checkbox_checked_count = 0;
    // var log = 'Checked: ';
    // for (var i = 0; i < countColumn; i++) {
    //     var checkbox = document.getElementById('step2Checkbox' + i);
    //     if (checkbox && checkbox.checked) {
    //         checkbox_checked_count++;
    //         log += DATA_STEP2.order_column[i] + ' ';
    //     }
    // }
    var avg_loop_limit = document.getElementById('avg_loop_limit').value;
    var permutation_count = document.getElementById('permutation_count').value;
    var data = {
        tablename: DATA_STEP1.data.tablename,
        order_column: DATA_STEP2.order_column,
        permutation_count: permutation_count,
        avg_loop_limit: avg_loop_limit,
        header: DATA_STEP1.data.header
    };
    $.ajax({
        type: 'post',
        url: '/fairness/module2/step3',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            onNextButtonStep2Callback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        beforeSend: function () {
            startProgress('Running Fairness Improvement Module...');
        },
        complete: function () {
            // endProgress();
        },
    });
}

function onNextButtonStep2Callback(result) {
    DATA_STEP3 = result;
    console.log('DATA_STEP3', DATA_STEP3);
    initStep3();
}

function initStep3() {
    prepareStep();
}

function initStep3() {
    prepareStep();
    $('#step' + StepIdx + '-box').append(
        $('<hr>'),
        $('<div>').attr('class', 'row').attr('id', 'step' + StepIdx + '-box-row').append(
            $('<div>').attr('class', 'col').append(
                $('<table>').attr('id', 'step' + StepIdx + '-table').attr('class', 'table table-striped table-dark').append(
                    $('<thead>').append(
                        $('<tr>').append(
                            $('<th>').append('#'),
                            $('<th>').append('Column'),
                            $('<th>').append('SD'),
                            $('<th>').append('Classification'),
                            $('<th>').append('Rows'),
                            $('<th>').append('Avg'),
                            $('<th>').append('File'),
                            $('<th>').append('Next')
                        )
                    )
                )
            )
        ),
        $('<div>').attr('class', 'row mt-4').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-back' + StepIdx).attr('class', 'btn btn-dark mr-3')
                        .attr('onclick', 'onBackButtonStep' + StepIdx + '()').append('Back'),
                    $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                        .attr('onclick', 'onNextButtonStep' + StepIdx + '()').attr('disabled', 'true').append('Next')
                )
            )
        )
    );
    initStep3Callback();
}

/*
function initStep3() {
    prepareStep();
    var countColumn;
    if (DATA_STEP3.order_column) {
        countColumn = DATA_STEP3.order_column.length < Step2MaxColumn ? DATA_STEP3.order_column.length : Step2MaxColumn;
    }
    $('#step' + StepIdx + '-box').append(
        $('<hr>'),
        $('<div>').attr('class', 'row row-cols-' + countColumn).attr('id', 'step' + StepIdx + '-box-row')
    );
    for (var i = 0; i < countColumn; i++) {
        var sd = Number(DATA_STEP3.stddev[DATA_STEP3.order_column[i]]);
        sd = sd.toFixed(4);
        $('#step' + StepIdx + '-box-row').append(
            $('<div>').attr('class', 'col-md').append(
                $('<div>').attr('id', 'step3ChartCase1' + i),
                $('<div>').attr('class', 'text-right').append(
                    $('<small>').append(
                        $('<em>').append('SD: ' + sd)
                    )
                )
            )
        );
    }
    $('#step' + StepIdx + '-box').append(
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    $('<span>').attr('class', '').append('Total ' + DATA_STEP3.rows.format() + ' rows.'),
                    $('<button>').attr('type', 'button').attr('class', 'btn btn-light ml-3')
                        .attr('onclick', 'downloadStep3Result()').append('Download'),
                )
            )
        ),
        $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case2}'),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('id', 'step3ChartCase2')
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    $('<span>').attr('class', '').append('Total ' + DATA_STEP3.rows.format() + ' rows.')
                )
            )
        ),
        $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case3}'),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('id', 'step3ChartCase3')
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    $('<span>').attr('class', '').append('Total ' + DATA_STEP3.rows.format() + ' rows.')
                )
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-back' + StepIdx).attr('class', 'btn btn-dark mr-3')
                        .attr('onclick', 'onBackButtonStep' + StepIdx + '()').append('Back'),
                    $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                        .attr('onclick', 'onNextButtonStep' + StepIdx + '()').append('Next')
                )
            )
        )
    );
    initStep3Callback();
}
 */

function initStep3Callback() {
    // for (var i = 0; i < Step2MaxColumn; i++) {
    //     drawStep2ChartCase1(DATA_STEP3, 'step3ChartCase1', i);
    // }
    endProgress();
    step3DataTable();
}

function onBackButtonStep3() {
    rewindStep();
    DATA_STEP3 = undefined;
    var range1 = document.getElementById('avg_loop_limit');
    if (range1) {
        range1.disabled = false;
    }
    var range2 = document.getElementById('permutation_count');
    if (range2) {
        range2.disabled = false;
    }
    // for (var i = 0; i < Step2MaxColumn; i++) {
    //     var checkbox = document.getElementById('step2Checkbox' + i);
    //     if (checkbox) {
    //         checkbox.disabled = false;
    //     }
    // }
}

function step3DataTable() {
    initStep3Table(initStep3TableData());
}

function initStep3Table(data) {
    $('#step3-table').DataTable({
        info: false,
        ordering: false,
        searching: false,
        lengthChange: false,
        pagingType: 'numbers',
        paging: false,
        pageLength: 5,
        data: data,
        columns: [
            {data: 'index'},
            {data: 'column'},
            {data: 'sd'},
            {data: 'count'},
            {data: 'sum'},
            {data: 'avg'},
            {
                data: 'download',
                render: function (data, type, row) {
                    return '<a href=\'' + data + '\'>' + uriToFileName(data) + '</a>';
                }
            },
            {
                title: 'Next',
                render: function (data, type, row) {
                    return '<button type=\'button\' class=\'btn btn-sm btn-info\' onclick="onNextButtonStep3(\'' + row.table + '\')">Next</button>';
                }
            }
        ]
    });
}

function initStep3TableData() {
    var data = [];
    for (var i = 0; i < DATA_STEP3.stats.length; i++) {
        var stats = DATA_STEP3.stats[i];
        var obj = {};
        obj.index = i + 1;
        obj.sd = stats.sd;
        obj.sum = stats.sum;
        obj.avg = stats.avg;
        obj.count = stats.count;
        var searchResult = searchColumnList(stats.tableName);
        obj.column = searchResult.column;
        obj.download = searchResult.download;
        obj.table = stats.tableName;
        data.push(obj);
    }
    return data;
}

function searchColumnList(key) {
    var obj = {};
    obj.column = obj.download = '';
    for (var i = 0; i < DATA_STEP3.tables.length; i++) {
        var table = DATA_STEP3.tables[i];
        if (table.tableName == key) {
            obj.column = table.executeColumnList.join(', ').toUpperCase();
            obj.download = table.csv.downloadUri;
            return obj;
        }
    }
    return undefined;
}

function uriToFileName(uri) {
    var arr = uri.split('/');
    return arr[arr.length - 1];
}

/*
function onNextButtonStep3() {
    var data = {
        tablename: DATA_STEP3.tablename,
        filename: DATA_STEP1.origin,
        header: DATA_STEP1.data.header
    };
    $.ajax({
        type: 'post',
        url: '/fairness/module2/step4',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            onNextButtonStep3Callback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        beforeSend: function () {
            startProgress();
        },
        complete: function () {
            // endProgress();
        },
    });
}

function onNextButtonStep3Callback(result) {
    DATA_STEP4 = result;
    console.log('DATA_STEP4', DATA_STEP4);
    initStep4();
}
 */

function onNextButtonStep3(tableName) {
    DATA_STEP4 = {};
    var btns = document.getElementsByClassName('btn btn-sm btn-info');
    for (var i = 0; i < btns.length; i++) {
        var btn = btns[i];
        if (btn) {
            btn.disabled = true;
        }
    }
    for (var i = 0; i < DATA_STEP3.stats.length; i++) {
        var stats = DATA_STEP3.stats[i];
        if (stats.tableName == tableName) {
            DATA_STEP4.stats = stats;
            break;
        }
    }
    for (var i = 0; i < DATA_STEP3.tables.length; i++) {
        var table = DATA_STEP3.tables[i];
        if (table.tableName == tableName) {
            DATA_STEP4.table = table;
            break;
        }
    }
    console.log('DATA_STEP4', DATA_STEP4);
    initStep4();
}

function initStep4() {
    prepareStep();
    $('#step' + StepIdx + '-box').append(
        // $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case4}'),
        // $('<div>').attr('class', 'row').append(
        //     $('<div>').attr('class', 'col').append(
        //         $('<div>').attr('id', 'step4ChartCase2')
        //     )
        // ),
        // $('<div>').attr('class', 'row').append(
        //     $('<div>').attr('class', 'col').append(
        //         $('<div>').attr('class', 'mt-3').append(
        //             $('<span>').attr('class', '').append('Total ' + DATA_STEP4.stats.sum.format() + ' rows.')
        //         )
        //     )
        // ),
        // $('<div>').attr('class', 'mt-3 todo').append('{Title.Chart.Case4}'),
        $('<div>').attr('class', 'row mt-3').append(
            $('<div>').attr('class', 'col').attr('style', 'height: 750px').append(
                $('<div>').attr('id', 'step4ChartCase3')
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-3').append(
                    $('<span>').attr('class', '').append('Total ' + DATA_STEP4.stats.sum.format() + ' rows.')
                )
            )
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-5 w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-back' + StepIdx).attr('class', 'btn btn-dark mr-3')
                        .attr('onclick', 'onBackButtonStep' + StepIdx + '()').append('Back'),
                    $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                        .attr('onclick', 'onNextButtonStep' + StepIdx + '()').append('Next')
                )
            )
        )
    );
    endProgress();
    getStep4ChartCase2Data();
}

function getStep4ChartCase2Data() {
    var data = {
        tablename: DATA_STEP4.table.tableName,
        columnlist: DATA_STEP4.table.columnList
    };
    $.ajax({
        type: 'post',
        url: '/fairness/module2/step4Chart2Data',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            getStep4ChartCase2DataCallback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        beforeSend: function () {
            startProgress('Running Result Analysis Module...');
        },
        complete: function () {
            // endProgress();
        },
    });
}

function getStep4ChartCase2DataCallback(result) {
    // drawStep4ChartCase2(result);
    drawStep4ChartCase3(result);
}

// Step4.Chart.Case2: d3 Icicle (https://observablehq.com/@d3/icicle, https://github.com/vasturiano/icicle-chart)
function drawStep4ChartCase2(data) {
    const width = window.innerWidth - 100;
    const color = d3.scaleOrdinal(d3.schemePaired);
    Icicle()
        .orientation('lr')
        .data(data)
        .size('size')
        .width(width)
        .color((d, parent) => color(parent ? parent.data.name : null))
        .excludeRoot(true)
        (document.getElementById('step4ChartCase2'));
}

// Step4.Chart.Case3: d3 Sunburst (https://observablehq.com/@kerryrodden/sequences-sunburst, https://github.com/vasturiano/sunburst-chart)
function drawStep4ChartCase3(data) {
    const color = d3.scaleOrdinal(d3.schemePaired);
    Sunburst()
        .data(data)
        .size('size')
        .height(732)
        .color((d, parent) => color(parent ? parent.data.name : null))
        .excludeRoot(true)
        .radiusScaleExponent(1)
        (document.getElementById('step4ChartCase3'));
}

function onBackButtonStep4() {
    rewindStep();
    DATA_STEP4 = undefined;
    var btns = document.getElementsByClassName('btn btn-sm btn-info');
    for (var i = 0; i < btns.length; i++) {
        var btn = btns[i];
        if (btn) {
            btn.disabled = false;
        }
    }
    var domC = document.getElementById('btn-next3');
    if (domC) {
        domC.disabled = true;
    }
}

function onNextButtonStep4() {
    initStep5();
}

function initStep5() {
    prepareStep();
    $('#step' + StepIdx + '-box').append(
        $('<div>').append(
            $('<ul>').append(
                $('<li>').append('File Name : ' + DATA_STEP4.table.csv.fileName),
                $('<li>').append('Algorithm Column : ' + DATA_STEP4.table.executeColumnList.join(', ')),
                $('<li>').append('Download URL : ' + DATA_STEP4.table.csv.downloadUri),
                $('<li>').append('Columns : ' + DATA_STEP4.table.columnList.join(', ')),
                $('<li>').append('Rows : ' + DATA_STEP4.stats.sum.format())
            ),
            $('<button>').attr('class', 'btn btn-info').attr('onclick', 'onStep5Download()').append('Download')
        ),
        $('<div>').attr('class', 'row').append(
            $('<div>').attr('class', 'col').append(),
            $('<div>').attr('class', 'col').append(
                $('<div>').attr('class', 'mt-5 w-100 text-right').append(
                    $('<button>').attr('type', 'button').attr('id', 'btn-back' + StepIdx).attr('class', 'btn btn-dark mr-3')
                        .attr('onclick', 'onBackButtonStep' + StepIdx + '()').append('Back'),
                    // $('<button>').attr('type', 'button').attr('id', 'btn-next' + StepIdx).attr('class', 'btn btn-info')
                    //     .attr('onclick', 'onNextButtonStep' + StepIdx + '()').append('Next')
                )
            )
        )
    );
    endProgress();
}

function onStep5Download() {
    window.location.assign(DATA_STEP4.table.csv.downloadUri);
}

function onBackButtonStep5() {
    rewindStep();
    DATA_STEP5 = undefined;
}

function onNextButtonStep5() {
    var data = {
        tablename: DATA_STEP1.data.tablename
    };
    $.ajax({
        type: 'get',
        // url: '/fairness/module2/report',
        url: '/getNow',
        // data: JSON.stringify(data),
        // dataType: 'json',
        // contentType: 'application/json',
        success: function (result) {
            onNextButtonStep5Callback(result);
        },
        error: function (error) {
            console.log(error);
            serverError(error);
        },
        // beforeSend: function () {
        //     startProgress();
        // },
        complete: function () {
            // endProgress();
        },
    });
}

function onNextButtonStep5Callback(result) {
    DATA_REPORT = result;
    console.log('DATA_REPORT', DATA_REPORT);
    openModal(2);
}

function serverError(error) {
    endProgress();
    document.getElementById('error-message').innerHTML = error.responseText;
    openModal(3);
}
