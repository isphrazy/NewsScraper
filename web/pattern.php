<?php
function print_head() {
?>
	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN""http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml">
		<head>
			<meta name="reverb news" />
			<title>Reverb News Center</title>
			<link href="index.css" type="text/css" rel="stylesheet" />
			<script src="http://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js" type="text/javascript"></script>
			<script src="index.js" type="text/javascript"></script>
		</head>
		<body>
	<?php	
}

function print_bottom(){
?>
		</body>
	</html>
<?php
}

function print_args_form(){
?>
	<div>
		Arg1: <input type="text" id="arg1" />
		Relations: <input type="text" id="rel" />
		Arg2: <input type="text" id="arg2" />
		<button id="search_submit">search</button>
	</div>
<?php	
	
}

?>

