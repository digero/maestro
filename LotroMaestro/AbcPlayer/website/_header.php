<?php
	$host_and_uri = $_SERVER['HTTP_HOST'] . rtrim(dirname($_SERVER['PHP_SELF']), '/\\');

	if (!defined('_ABCPLAYER_'))
	{
		header("Location: http://$host_and_uri/index.php");
		exit();
	}
	
	function sidebar($href, $text)
	{
		echo('<a ');
		if (basename($_SERVER['PHP_SELF']) == $href)
			echo('class="active" ');
		echo('href="' . $href . '">' . $text . "</a>\n");
	}
	
	function mp3($filename, $title)
	{
		global $host_and_uri;
		echo("<div><div id='$filename'><a href='http://$host_and_uri/mp3/$filename.mp3'>$filename.mp3</a></div></div>\n");
		echo("<script type='text/javascript'>");
		echo("AudioPlayer.embed('$filename', {soundFile: 'http://$host_and_uri/mp3/$filename.mp3'});");
		echo("</script>\n");
	}
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>

<head>
<title>ABC Player</title>

<link rel="stylesheet" href="abcplayer.css" type="text/css" />
<link rel="icon" type="image/vnd.microsoft.icon" href="abcplayer.ico" />

<script type="text/javascript" src="mp3/audio-player.js"></script>
<script type="text/javascript">
	AudioPlayer.setup("http://<?= $host_and_uri ?>/mp3/player.swf", {
		width: 438, 
		transparentpagebg: "yes", 
	});
</script>
</head>

<body>
<div id="page">
	<div id="header">
		<img id="headericon" src="images/abcplayer_96.png" />
		<h1><a href="index.php">ABC Player</a></h1>
		<div class="description">for <span style="font-style: italic;">The Lord of the Rings Online<span></div>
	</div>
	
	<div id="mainarea">
	<div id="sidebar">
		<div id="sidebarnav">
		<?php sidebar('index.php', 'Home'); ?>
		<?php sidebar('changelog.php', 'Change Log'); ?>
		<?php sidebar('contact.php', 'Contact Me'); ?>
		</div>
		<div id="donate">
		<form action="https://www.paypal.com/cgi-bin/webscr" method="post">
			<input type="hidden" name="cmd" value="_s-xclick">
			<input type="hidden" name="hosted_button_id" value="KPX5YZW5SALAG">
			<input type="image" src="https://www.paypal.com/en_US/i/btn/btn_donate_SM.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
			<img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1">
		</form>
		</div>
	</div>
	
	<div id="contentarea">
