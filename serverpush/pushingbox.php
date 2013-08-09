<?php
/*
create custom service 'newtifry'
https://newtifry.appspot.com/newtifry
POST

create scenario : ?format=json&source=$source$&title=$title$&message=$message$

*/

function newtifryPushingBox($devid, $source, $title, $message) {
    $url = 'http://api.pushingbox.com/pushingbox?devid=' . $devid . '&source=' . $source . '&message=' . urlencode($message) . '&title=' . urlencode($title); 
    $ch = curl_init($url);
    curl_exec ($ch);
    curl_close ($ch);
}


// call example 
$pushingboxDevid = "YOURpushingboxDevid";
$newtifrySource = "YOURnewtifrySource";
newtifryPushingBox($pushingboxDevid, $newtifrySource, "portail", "fermeture");

?>
