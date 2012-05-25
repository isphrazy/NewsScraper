window.onload = function(){
	$("search_submit").observe("click", search_news);	
};

function search_news(){
	var arg1_text = $("arg1").value;
	var rel_text = $("rel").value;
	var arg2_text = $("arg2").value;
	//alert(arg1_text);
	//alert(rel_text);
	//alert(arg2_text);
	new Ajax.Request("search_extract.php", {
		method: "get",
		parameters: {arg1: arg1_text,
					 rel: rel_text,
					 arg2: arg2_text},
		onSuccess:update_search
	});
}

function update_search(ajax){
//alert("done");
	alert(ajax.responseText.trim());
}
