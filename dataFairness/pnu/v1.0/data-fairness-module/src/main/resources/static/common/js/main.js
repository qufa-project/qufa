$(document).ready(function () {
    $('#modal-1').on('hide.bs.modal', function () {
        document.getElementById('modal-1-form').reset();
        // stopProcessMessage('modal-1-processing', 'Done', modal1Interval);
        // document.getElementById('modal-1-processing').innerHTML = '';
    });
    $('#modal-4').on('hide.bs.modal', function () {
        redrawChart();
    });
    init();
});

google.charts.load('current', {'packages':['timeline']});
google.charts.setOnLoadCallback(drawTimeline);
var tl_container, tl_chart, tl_dataTable, tl_sum = 0;
var modal1Interval;
var filenames = [];
var convertData = {};

function init() {
    var c = 5;
    var tail;
    setInterval(function() {
        var a = (c % 5);
        tail = '.';
        for (var i = 0; i < a; i++) {
            tail += '.';
        }
        document.getElementById('running').innerHTML = "Running" + tail + "";
        c++;
    }, 500);
}

function lockFormUpload() {
    document.getElementById('file1').disabled = true;
    document.getElementById('except').disabled = true;
    document.getElementById('btn-upload').disabled = true;
}

function unlockFormUpload() {
    document.getElementById('file1').disabled = false;
    document.getElementById('except').disabled = false;
    document.getElementById('btn-upload').disabled = false;
}

function onUpload() {
    var form = document.getElementById('form-upload');
    var data = new FormData(form);
    $.ajax({
        type: 'post',
        enctype: 'multipart/form-data',
        url: '/api/compensation/submitUpload',
        data: data,
        processData: false,
        contentType: false,
        cache: false,
        success: function (result) {
            onUploadCallback(result);
        },
        error: function (error) {
            console.log(error);
        },
        xhr: function () {
            var xhr = new window.XMLHttpRequest();
            $('#progress').css('width', '0');
            xhr.upload.addEventListener("progress", function (evt) {
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

function onUploadCallback(result) {
    console.log(result);
    setTimeout(function () {
        $('#progress').css('width', '0');
    }, 500);
    updateCount();
    lockFormUpload();
    initTimeline(result);
    drawModal1Table(result);
    onModal1Show();
}

function drawModal1Table(result) {
    var dom1 = document.getElementById('origin');
    if (dom1) {
        dom1.value = result.origin;
    }
    var dom2 = document.getElementById('filename');
    if (dom2) {
        dom2.value = result.filename;
    }
    var dom3 = document.getElementById('modal-1-title');
    if (dom3) {
        dom3.innerHTML = result.origin;
    }
    var tbody = document.getElementById('modal-1-tbody');
    for (var i = tbody.rows.length; i > 0; i--) {
        tbody.deleteRow(i - 1);
    }
    var columns = result.data.columns;
    var example = result.data.example;
    for (var i = 0; i < columns; i++) {
        var row = tbody.insertRow(tbody.rows.length);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        var cell3 = row.insertCell(2);
        var cell4 = row.insertCell(3);
        var cell5 = row.insertCell(4);
        cell1.innerHTML = String(i + 1);
        cell2.innerHTML = '<input type="checkbox" name="select-column" class="form-control" value="' + i + '" checked>';
        cell3.innerHTML = example[i];
        cell4.innerHTML = '<input type="text" name="exclude" class="form-control">';
        cell5.innerHTML = '<input type="text" name="include" class="form-control">';
    }
}

function onConvert() {
    startProcessMessage('modal-1-processing', 'Processing', '.', 5);
    var s = getCheckedColumn();
    if (!s.length) {
        stopProcessMessage('modal-1-processing', 'No column selected.', modal1Interval);
        return false;
    }
    document.getElementById('select').value = s.join(',');
    var form = document.getElementById('modal-1-form');
    var data = new FormData(form);
    $.ajax({
        type: 'post',
        url: '/api/compensation/submitConvert',
        data: data,
        processData: false,
        contentType: false,
        cache: false,
        success: function (result) {
            onConvertCallback(result);
        },
        error: function (error) {
            console.log(error);
        },
        xhr: function () {
            var xhr = new window.XMLHttpRequest();
            $('#modal-1-progress').css('width', '0');
            xhr.upload.addEventListener("progress", function (evt) {
                if (evt.lengthComputable) {
                    var percentComplete = evt.loaded / evt.total;
                    percentComplete = parseInt(percentComplete * 100);
                    // console.log(percentComplete);
                    $('#modal-1-progress').css('width', percentComplete + '%');
                    if (percentComplete === 100) {
                    }
                }
            }, false);
            return xhr;
        },
    });
    return false;
}

function onConvertCallback(result) {
    console.log(result);
    setTimeout(function () {
        $('#modal-1-progress').css('width', '0');
    }, 500);
    filenames.push(result.filename);
    tl_sum += result.data.rows;
    updateCount();
    stopProcessMessage('modal-1-processing', 'Done', modal1Interval);
    $('#modal-1-close').trigger('click');
    var row = [
        result.filename,
        result.data.rows.toLocaleString() + ' rows',
        '',
        0,
        result.data.rows
    ];
    addRowTimeline(row);
    drawModal1Table(result);
}

function toggleColumnAll() {
    var a = document.getElementsByName('select-column');
    var b = document.getElementById('toggle-checkbox');
    var l = a.length;
    for (var i = 0; i < l; i++) {
        a[i].checked = b.checked;
    }
}

function syncExcept() {
    var c = document.getElementById('except');
    var v;
    if (c.checked) {
        v = 'true';
    } else {
        v = 'false';
    }
    document.getElementById('except2').value = v;
}

function startProcessMessage(id, message, suffix, count) {
    var dom = document.getElementById(id);
    var c = count;
    var tail;
    modal1Interval = setInterval(function() {
        var a = (c % count);
        tail = suffix;
        for (var i = 0; i < a; i++) {
            if (suffix) tail += suffix;
        }
        dom.innerHTML = "<kbd>" + message + tail + "</kbd>"
        c++;
    }, 500);
}

function stopProcessMessage(id, message, interval) {
    if (interval) {
        clearInterval(interval);
    }
    var dom = document.getElementById(id);
    if (id && message) {
        dom.innerHTML = "<kbd>" + message + "</kbd>"
    }
    setTimeout(function() {
        dom.innerHTML = '';
    }, 500);
}

function modalProcessMessage(id, message) {
    var dom = document.getElementById(id);
    dom.innerHTML = message;
}

function onModal1Show() {
    $('#modal-1-button').trigger('click');
    return false;
}

function initTimeline(result) {
    $('#timeline-container').slideDown();
    tl_container = document.getElementById('timeline');
    tl_chart = new google.visualization.Timeline(tl_container);
    tl_dataTable = new google.visualization.DataTable();
    tl_dataTable.addColumn({ type: 'string', id: 'Filename' });
    tl_dataTable.addColumn({ type: 'string', id: 'dummy bar label' });
    tl_dataTable.addColumn({ type: 'string', role: 'tooltip' });
    tl_dataTable.addColumn({ type: 'number', id: 'Start' });
    tl_dataTable.addColumn({ type: 'number', id: 'End' });
    // google.visualization.events.addListener(tl_chart, 'select', function() {
    //     var selection = tl_chart.getSelection();
    //     if (selection.length > 0 && selection[0].row > 0) {
    //         document.getElementById('remove-row-index').value = selection[0].row;
    //         $('#modal-4-button').trigger('click');
    //     }
    // });
    google.visualization.events.addListener(tl_chart, 'ready', function() {
        var labels = tl_container.getElementsByTagName('text');
        Array.prototype.forEach.call(labels, function(label) {
            if (label.getAttribute('text-anchor') === 'middle') {
                label.setAttribute('fill', '#EAECEE');
            }
            // if (label.getAttribute('text-anchor') === 'end') {
            //     label.setAttribute('style', 'overflow:visible; text-overflow:clip; word-break:break-all; word-wrap:break-word; white-space:nowrap;');
            // }
        });
    });
    var row = [
        result.filename,
        result.data.rows.toLocaleString() + ' rows',
        '',
        0,
        result.data.rows
    ];
    addRowTimeline(row);
}

function drawTimeline() {
}

function addRowTimeline(row) {
    tl_dataTable.addRows([row]);
    tl_chart.draw(tl_dataTable, getChartOption());
}

function removeRowTimeline() {
    var i = Number(document.getElementById('remove-row-index').value);
    tl_sum -= convertNumber(tl_dataTable.getValue(i, 1));
    filenames.splice(i - 1, 1);
    tl_dataTable.removeRow(i);
    tl_chart.draw(tl_dataTable, getChartOption());
    updateCount();
    $('#modal-4-close').trigger('click');
}

function onReset() {
    $('#modal-2-button').trigger('click');
}

function onResetAction() {
    convertData = {};
    convertData.filename = '';
    convertData.convert = [];
    unlockFormUpload();
    tl_chart.clearChart();
    $('#timeline-container').slideUp();
    document.getElementById('form-upload').reset();
    document.getElementById('modal-1-form').reset();
    document.getElementById('origin').value = '';
    document.getElementById('filename').value = '';
    document.getElementById('except2').value = 'true';
    document.getElementById('select').value = '';
    document.getElementById('remove-row-index').value = '';
    modalProcessMessage('modal-1-processing', '<kbd>0 Added.</kbd>');
    tl_sum = 0;
    filenames = [];
    $('#modal-2-close').trigger('click');
}

function onDownload() {
    if (!filenames.length) {
        $('#modal-3-button').trigger('click');
        return false;
    }
    var data = {
        "filenames": filenames,
        "origin": document.getElementById('origin').value,
        "except": document.getElementById('except2').value
    };
    console.log(data);
    $.ajax({
        type: 'post',
        url: '/api/compensation/submitDownload',
        data: data,
        traditional: true,
        success: function (result) {
            onDownloadCallback(result);
        },
        error: function (error) {
            console.log(error);
        },
    });
}

function onDownloadCallback(result) {
    console.log(result);
    window.location.assign(result.fileurl);
}

function updateCount() {
    // document.getElementById('count-rows').innerText = 'Total ' + tl_sum.toLocaleString() + ' rows.';
}

function convertNumber(s) {
    return Number(s.replace(/[^0-9]/g, ''));
}

function redrawChart() {
    tl_chart.draw(tl_dataTable, getChartOption());
}

function getChartOption() {
    return {
        height: 61.5 + (61.5 * (tl_dataTable.getNumberOfRows())),
        timeline: {
            // rowLabelStyle: { fontSize: 12 },
            barLabelStyle: { fontSize: 18 }
        },
        // hAxis: {
        //     ticks: []
        // }
    };
}

function getCheckedColumn() {
    var s = [];
    var cnt = document.getElementsByName("select-column").length;
    for (var i = 0; i < cnt; i++) {
        if (document.getElementsByName("select-column")[i].checked === true) {
            var val = document.getElementsByName("select-column")[i].value;
            s.push(val);
        }
    }
    return s;
}

function getCheckedColumnV2() {
    var s = [];
    var cnt = document.getElementsByName("select-column").length;
    for (var i = 0; i < cnt; i++) {
        var v;
        if (document.getElementsByName("select-column")[i].checked === true) {
            v = 'true';
        } else {
            v = 'false';
        }
        s.push(v);
    }
    return s;
}

function getExcludeColumn() {
    var s = [];
    var cnt = document.getElementsByName("exclude").length;
    for (var i = 0; i < cnt; i++) {
        var val = document.getElementsByName("exclude")[i].value.trim();
        s.push(val);
    }
    return s;
}

function getIncludeColumn() {
    var s = [];
    var cnt = document.getElementsByName("include").length;
    for (var i = 0; i < cnt; i++) {
        var val = document.getElementsByName("include")[i].value.trim();
        s.push(val);
    }
    return s;
}

function getLimitRow() {
    return document.getElementsByName('limit')[0].value.trim();
}

function addConvertData() {
    convertData.filename = document.getElementById('filename').value;
    convertData.except = document.getElementById('except2').value;
    if (!convertData.convert) {
        convertData.convert = [];
    }
    var data = {};
    data.select = getCheckedColumnV2();
    data.exclude = getExcludeColumn();
    data.include = getIncludeColumn();
    data.limit = getLimitRow();
    var checked = getCheckedColumn();
    if (checked.length) {
        convertData.convert.push(data);
        console.log(convertData);
        var len = convertData.convert.length;
        var row = [
            'Step ' + len,
            '',
            '',
            0,
            0
        ];
        addRowTimeline(row);
        document.getElementById('modal-1-form').reset();
        modalProcessMessage('modal-1-processing', '<kbd>' + len + ' Added.</kbd>');
        // stopProcessMessage('modal-1-processing', 'Added.', modal1Interval);
    } else {
        // stopProcessMessage('modal-1-processing', 'No column selected.', modal1Interval);
    }
    return false;
}

function onConvertRun() {
    if (!convertData.convert || !convertData.convert.length) {
        $('#modal-5-button').trigger('click');
        return false;
    }
    $('#modal-6-button').trigger('click');
    $.ajax({
        url: '/api/compensation/convertRun',
        method: 'post',
        data: JSON.stringify(convertData),
        dataType: 'json',
        contentType: 'application/json',
        success: function (result) {
            onConvertRunCallback(result);
        },
        error: function (err) {
            console.log(err);
        }
    });
    return false;
}

function onConvertRunCallback(result) {
    console.log(result);
    var len = result.convert.length;
    if (len) {
        for (var i = len; i > 0; i--) {
            tl_dataTable.removeRow(i);
            tl_chart.draw(tl_dataTable, getChartOption());
        }
        for (var i = 0; i < len; i++) {
            var convert = result.convert[i];
            var row = [
                convert.filename,
                convert.data.rows.toLocaleString() + ' rows',
                '',
                0,
                convert.data.rows
            ];
            addRowTimeline(row);
        }
    }
    setTimeout(function() {
        $('#modal-6').modal('hide');
        if (result.fileurl) {
            window.location.assign(result.fileurl);
        }
    }, 700);
}
