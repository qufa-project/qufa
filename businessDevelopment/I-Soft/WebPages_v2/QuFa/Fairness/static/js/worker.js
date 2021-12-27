
onmessage = function(e)
{
    var url = "../../Fairness/run_alg/";
    var objData = e.data; 
    var xhr = new XMLHttpRequest();
    xhr.open("POST", url, false);
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