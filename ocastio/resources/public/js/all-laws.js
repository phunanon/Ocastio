//example: javascript:doPOST('http://is.gd/create.php', {'URL': location.href});
function doPOST (url, params)
{
    var formData = new FormData();
    for (var p in params)
        if (params.hasOwnProperty(p))
            formData.append(p, params[p]);
    //Copy anti-forgery token from another form on the page
    formData.append("__anti-forgery-token", document.querySelector("form input").value);
    //
    var request = new XMLHttpRequest();
    request.open("POST", url);
    request.send(formData);
}

function deleteLaw (law_id, delement)
{
    doPOST("delete/law", {"law_id": law_id});
    delement.parentNode.parentNode.style.backgroundColor = "#faa";
}
