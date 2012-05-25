<?php

define (SOURCE_FILE, 'data/meta_data/2012-05-10_yahoo_rss.data');
define (TODAY_FILE, 'data/today_news/today_news.data');
define (MAX_NEWS_NUMBER, 50);
define (MAX_TITLE_SIZE, 50);

$simple_news = array();
$key_fields = array('title', 'url', 'content', 'date');
generate_today_news();
//output_data();

print_data();

function print_data(){
	print file_get_contents(TODAY_FILE);
}

function output_data(){
	global $simple_news;

	$file = TODAY_FILE;
	$fh = fopen($file, 'w') or die("can't open file");
	fwrite($fh, json_encode($simple_news));
	fclose($fh);
}

function generate_today_news(){
	global $simple_news;
	global $key_fields;

	$file = get_file_json();
	$count = 0;
	foreach($file as $new_id => $entry){
		if($count > MAX_NEWS_NUMBER) break;
		$news_info = array();
		foreach($key_fields as $key_field){
			//$s = $entry[]
			if($key_field == 'title'){
				
			}
			$news_info[$key_field] = $entry[$key_field];	
		}
		array_push($simple_news, $news_info);
		$count ++;
	}
}

//get the file content, and return the content in json decoded format
function get_file_json(){
	
	$meta_data_file = file_get_contents(SOURCE_FILE);
	return json_decode($meta_data_file, true);	
		
}

?>
