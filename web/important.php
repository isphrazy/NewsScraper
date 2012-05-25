<?php

define('META_DATA_DIR', 'data/meta_data/');
define('MAX_CONTENT_SIZE', 255);

$json_keywords = array('title', 'imgAlt', 'imgTitle', 'content');
$json_importance_key_fields = array('title', 'url', 'content');
$garbage_words = get_garbage_words();
$words_weight = array();


//process the file with given time interval
//not support date in this version
function get_words_weight($start_date, $end_date){
	global $words_weight;
	
	$files = glob(META_DATA_DIR . "*.data");
	foreach($files as $file){
		//get_file_time($file);
		$json_file = load_meta_data_json($file);
		process_json_to_weight($json_file);
	}

	uasort($words_weight, 'words_weight_comparision');
	$words_weight = array_slice($words_weight, 2, 32);

	$result = array();
	foreach($words_weight as $keyword => $values){
		$entry = array();
		$entry['keyword'] = $keyword;
		foreach($values as $key => $value){
			$entry[$key] = $value;
		}
		array_push($result, $entry);
	}
//
//	foreach($words_weight as $word => $word_info){
//		if($word !== 0 && $word !== 1 && $word !== 2)
//			print $word	. ": " . $word_info["count"] . "<br/>";
//	//	print "\t" . var_dump($word_info["info"]) . "<br/>";
//	}

	//print json_encode($words_weight);

	write_words_weight_to_file(json_encode($result));
	
}

function print_data(){
	print file_get_contents('data/important_data/today.imp');
}

//write the given file to disk
function write_words_weight_to_file($string){
	$my_file = "data/important_data/today.imp";
	$fh = fopen($my_file, 'w') or die("can't open file");
	fwrite($fh, $string);
	fclose($fh);	
}

//get words weight from given json file
function process_json_to_weight($json_file){
	global $json_keywords;
	global $words_weight;
	
	//for each news
	foreach($json_file as $news_data_id => $news_entry){

		$key_fields = get_key_fields($news_entry);
		//for each keyword in $json_keywords
		foreach($json_keywords as $keyword){
			scrape_keywords($key_fields, $news_entry[$keyword]);
			
		}
	}	
}

//compare two words
function words_weight_comparision($a, $b){
	return	$a["count"]<$b["count"];	
}

//store the key fields in an associate array, and return it
function get_key_fields($news_entry){
	global $json_importance_key_fields;
	
	$key_fields	= array();
	foreach($json_importance_key_fields as $ji_key_field){
		$s = $news_entry[$ji_key_field];
		if($ji_key_field == 'content'){
			$s = substr($s, 0, MAX_CONTENT_SIZE); 
			$s .= ' ...';	
		}
		$key_fields[$ji_key_field] = $s;
	}
	return $key_fields;
	
}

//calculate the weight of the keywords in the given paragraph
function scrape_keywords($key_fields, $paragraph){
	global $words_weight;
	global $garbage_words;
	
	$words = preg_split("/[\n\r\t ,!.\?\'\";:()\[\]{}<>=]+/", $paragraph);
	foreach($words as $word){
		$word = ucfirst($word);
		if(!in_array($word, $garbage_words)){

			if(!isset($words_weight[$word])){
				$words_weight[$word] = array("count"=>1, "info"=>array($key_fields));
			}else{
				$words_weight[$word]["count"] ++;
				if(!in_array($key_fields, $words_weight[$word]["info"]))
					array_push($words_weight[$word]["info"], $key_fields);
			}
		}
		
	}
}

//load the given json file into content
function load_meta_data_json($file_name){
	$meta_data_file = file_get_contents($file_name);
	return json_decode($meta_data_file, true);	
}

//return the array of garbage words
function get_garbage_words(){
	
	return array('', 'The', 'Of', 'To', 'A', 'In', 'And', 'On', 'S', 'For', '0', '000', '1', 'At', 'That', 'With', 'Is', 'By', 'As', 'From', 'Has', 'Said', 'An', 'Its', 'His', 'Was', 'It', 'Will', 'After', 'Are', 'New', 'He', 'Be', 'Their', 'Thursday', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December', '-', 'S', 'A', 'U', 'This', 'Not', 'About', 'Who', 'Up', 'Or', 'Have', 'More', 'First', 'But', 'Over', 'During', 'Which', 'Two', 'One', 'Three', 'Monday', 'Wednesday', 'Tuesday', 'Thursday', 'Friday', 'Saturday', 'Sunday', 'Into', 'Than', 'Year', 'Out', 'They', 'Percent', 'Were', 'Had', 'Her', 'Says', 'Time', 'Would', 'When', 'Last', '--', 'News', 'You', 'Could', 'No', 'Today', 'Most', 'All', 'Some', 'T', 'Day', 'Off', 'How', 'Now', 'Next', 'Just', 'Before', 'Since');
}

?>
