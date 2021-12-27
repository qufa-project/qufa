
onmessage = function(e)
{
    var objData = e.data; 
    var xhr = new XMLHttpRequest();
    xhr.open("POST", objData.url, false);
    xhr.setRequestHeader('X-CSRFToken', objData.csrfTtoken);
    xhr.setRequestHeader('Content-type', 'application/json')
    xhr.onreadystatechange = function()
    {
        if (this.readyState == XMLHttpRequest.DONE && this.status == 200)
        {
            postMessage(xhr.responseText);
        }
    };
    xhr.send(JSON.stringify(objData));

}